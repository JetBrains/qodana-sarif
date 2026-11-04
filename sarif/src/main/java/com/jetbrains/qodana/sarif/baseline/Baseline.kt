package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SHIFT_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.MOVE_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SCOPE_AND_SHAPE_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run
import java.util.IdentityHashMap

/**
 * Absolute context-snippet similarity a pair must reach to be committed in a phase whose key can pair unrelated
 * results.
 */
private const val SIMILARITY_FLOOR = 0.45

/** Report:baseline ratio from which a pool counts as unbalanced, and its pairs need evidence to be committed. */
private const val UNBALANCED_POOL_RATIO = 4

/** Non-null results that are not already marked absent. */
private fun Run.undecidedResults(): List<Result> =
    results.orEmpty().mapNotNull { result -> result?.takeIf { it.baselineState != BaselineState.ABSENT } }

/**
 * The result's unique id: equalIndicator/v1. Used as the candidate-pool key, and recorded on a matched report result as
 * `matchedBaselineFingerprint` (the fingerprint of the baseline problem it matched).
 */
private fun Result.uniqueResultIndicator(): String {
    val fingerprints = partialFingerprints ?: return ""
    return fingerprints.get(EQUAL_INDICATOR, 1) ?: fingerprints.getLastValue(EQUAL_INDICATOR) ?: ""
}

/**
 * The cloud-assigned id of a baseline problem, present when the baseline came from Cloud. Copied onto the report result that
 * matched it under the same `cloudId` key, so the id travels with the problem across runs rather than being
 * match metadata: any result carrying one is the same Cloud problem.
 */
internal fun Result.cloudId(): Any? = properties?.get("cloudId")

/** The presence of these hashes identifies a new-analyzer report, whereas their absence signifies a legacy baseline. */
private fun Result.hasHash(key: String): Boolean =
    partialFingerprints?.getLastValue(key)?.isNotEmpty() == true

internal class DiffState(
    private val options: Options,
    val undecidedFromReport: MutableMap<String, Result> = LinkedHashMap(),
    val undecidedFromBaseline: MutableMap<String, Result> = LinkedHashMap(),
) {
    var new = 0
        private set
    var unchanged = 0
        private set
    var absent = 0
        private set

    val results = mutableListOf<Result>()

    fun put(
        result: Result,
        state: BaselineState,
        matchedMethod: String? = null,
        baselineFingerprint: String? = null,
        cloudId: Any? = null,
    ): Boolean {
        if (state == BaselineState.UNCHANGED && !options.includeUnchanged) return false
        if (state == BaselineState.ABSENT && !options.includeAbsent) return false

        if (options.includeMatchedMethod && matchedMethod != null) result.updateProperties { it["matchedMethod"] = matchedMethod }
        if (cloudId != null) result.updateProperties { it["cloudId"] = cloudId }
        if (baselineFingerprint != null) {
            result.updateProperties { it["matchedBaselineFingerprint"] = baselineFingerprint }
        }
        results.add(result.withBaselineState(if (options.fillBaselineState) state else null))
        when (state) {
            BaselineState.NEW -> new++
            BaselineState.UNCHANGED -> unchanged++
            BaselineState.ABSENT -> absent++
            BaselineState.UPDATED -> Unit
        }
        return true
    }

    fun isMatchedMethodIncluded(): Boolean = options.includeMatchedMethod

    /** Whether neither endpoint has been taken yet, so the pair is still the matcher's to make. */
    fun isUnclaimed(candidate: MatchCandidate): Boolean =
        candidate.reportResult.uniqueResultIndicator() in undecidedFromReport &&
                candidate.baselineResult.uniqueResultIndicator() in undecidedFromBaseline

    /** Records an UNCHANGED match and consumes both endpoints (by their equalIndicator id) from the candidate pools. */
    fun commit(reportResult: Result, baselineResult: Result, matchedMethod: String) {
        put(
            reportResult,
            BaselineState.UNCHANGED,
            matchedMethod,
            baselineResult.uniqueResultIndicator(),
            baselineResult.cloudId(),
        )
        undecidedFromReport.remove(reportResult.uniqueResultIndicator())
        undecidedFromBaseline.remove(baselineResult.uniqueResultIndicator())
    }
}

/** CAUTION: This mutates results in report and baseline **/
internal fun applyBaseline(report: Run, baseline: Run, options: Options): DiffState {
    val reportDescriptors = DescriptorLookup(report)
    val baselineDescriptors = DescriptorLookup(baseline)

    val reportResults = report.undecidedResults()
    val baselineResults = baseline.undecidedResults()

    // shiftTolerantEqualIndicator is the analyzer-generated 1:1 equivalent of ResultKey content equality hash
    val hasUseShiftTolerantHash = baselineResults.any { it.hasHash(SHIFT_TOLERANT_INDICATOR) }
            || reportResults.any { it.hasHash(SHIFT_TOLERANT_INDICATOR) }

    if (!hasUseShiftTolerantHash) return applyBaselineOldAlg(report, baseline, options, DiffState(options))

    // The candidate pools are keyed by the unique equalIndicator id (=hash)
    val state = DiffState(
        options,
        reportResults.associateByTo(LinkedHashMap()) { it.uniqueResultIndicator() },
        baselineResults.associateByTo(LinkedHashMap()) { it.uniqueResultIndicator() },
    )

    // equalIndicator is a unique, collision-free key: no scoring or tiebreaking. The remaining hashes can collide
    // on a single result, so each later phase assigns its still-undecided results globally by match quality.
    matchEqualIndicatorPhase(state)
    // shiftTolerant means identical content, so it trusts the hash alone. Every key after it can pair unrelated
    // results, so its pairs must clear a floor.
    val featuresOf = ResultFeatures.Cache()
    for ((fingerprintKey, similarityFloor) in listOf(
        SHIFT_TOLERANT_INDICATOR to null,
        MOVE_AND_REFACTOR_TOLERANT_INDICATOR to SIMILARITY_FLOOR,
        EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to SIMILARITY_FLOOR,
        SCOPE_AND_SHAPE_TOLERANT_INDICATOR to SIMILARITY_FLOOR,
    )) {
        matchPhase(fingerprintKey, state, featuresOf, similarityFloor)
    }

    state.undecidedFromReport.values.forEach { state.put(it, BaselineState.NEW) }
    state.undecidedFromBaseline.values.forEach { result ->
        if (!options.wasChecked.apply(result)) {
            state.put(result, BaselineState.UNCHANGED)
        } else if (state.put(result, BaselineState.ABSENT) && reportDescriptors.findById(result.ruleId) == null) {
            baselineDescriptors.findById(result.ruleId)?.addTo(report)
        }
    }

    report.withResults(state.results)

    return state
}

/**
 * Commits exact matches from a unique, collision-free key (equalIndicator). Matched endpoints are removed from [state]'s pools in place.
 */
private fun matchEqualIndicatorPhase(state: DiffState) {
    val matcher = HashMatcher(state.undecidedFromBaseline.values, EQUAL_INDICATOR)
    for (candidate in matcher.candidates(state.undecidedFromReport.values)) {
        if (state.isUnclaimed(candidate)) {
            state.commit(candidate.reportResult, candidate.baselineResult, candidate.matchedMethod)
        }
    }
}

/**
 * Resolves one matching phase for a key that can collide on a single result.
 *
 * A pair below [similarityFloor] (`null` for no floor) is dropped first, so it can neither be committed nor make
 * its endpoints look contested. Of the rest, those sharing no endpoint are committed directly; the others are
 * assigned greedily, strongest score first, so a weaker pair cannot steal a baseline from a stronger one.
 *
 * A floor-bearing key is also one that can pair unrelated results, so there - and only there - a pair out of an
 * unbalanced pool (see [isPoolUnbalanced]) needs evidence (see [isRealEvidence]). Matched endpoints leave [state]'s
 * pools in place, so the leftovers flow to the next phase.
 */
private fun matchPhase(
    fingerprintKey: String,
    state: DiffState,
    featuresOf: ResultFeatures.Cache,
    similarityFloor: Double?,
) {
    val matcher = HashMatcher(state.undecidedFromBaseline.values, fingerprintKey)
    val allCandidates = matcher.candidates(state.undecidedFromReport.values)
    val candidates = if (similarityFloor == null) allCandidates else allCandidates.filter {
        featuresOf(it.reportResult).meetsSimilarityFloor(featuresOf(it.baselineResult), similarityFloor)
    }
    if (candidates.isEmpty()) return

    val baselinesByReport = IdentityHashMap<Result, MutableList<Result>>()
    val reportsPerBaseline = IdentityHashMap<Result, Int>()
    for ((reportResult, baselineResult) in candidates) {
        baselinesByReport.getOrPut(reportResult) { ArrayList() }.add(baselineResult)
        reportsPerBaseline.merge(baselineResult, 1, Int::plus)
    }

    val isLabelled = state.isMatchedMethodIncluded()

    val candidatesWithCollisions = ArrayList<MatchCandidate>(candidates.size)
    for (c in candidates) {
        if (baselinesByReport.getValue(c.reportResult).size == 1 && reportsPerBaseline.getValue(c.baselineResult) == 1) {
            state.commit(c.reportResult, c.baselineResult, c.matchedMethod)
        } else {
            candidatesWithCollisions.add(c)
        }
    }
    if (candidatesWithCollisions.isEmpty()) return

    // Score every collided edge exactly once; the same scores drive the greedy order and the label.
    val scoreByReportBaseline = IdentityHashMap<Result, IdentityHashMap<Result, MatchScore>>()
    candidatesWithCollisions
        .map { candidate ->
            val score = TiebreakerCascade.score(featuresOf(candidate.reportResult), featuresOf(candidate.baselineResult))
            scoreByReportBaseline.getOrPut(candidate.reportResult) { IdentityHashMap() }[candidate.baselineResult] = score
            candidate to score
        }
        .sortedByDescending { it.second }
        .forEach { (candidate, score) ->
            if (!state.isUnclaimed(candidate)) return@forEach

            val isUnbalanced = similarityFloor != null &&
                    isPoolUnbalanced(candidate, baselinesByReport, reportsPerBaseline)

            val decidingSignal = if (isUnbalanced || isLabelled) {
                score.decidingSignalAgainst(
                    strongestRivalScore(candidate, baselinesByReport, scoreByReportBaseline, state)
                )
            } else {
                null
            }
            if (isUnbalanced && !isRealEvidence(decidingSignal, featuresOf(candidate.reportResult))) return@forEach
            val suffix = if (isLabelled && decidingSignal != null) "+${decidingSignal.label}" else ""
            state.commit(candidate.reportResult, candidate.baselineResult, candidate.matchedMethod + suffix)
        }
}

/**
 * The score of the strongest baseline the pair's report result still competes for, other than the one it takes.
 *
 * Only this side is asked, so a pool where every report result wants the one baseline yields no rival and commits
 * nothing. Asking the report side too was measured and bought nothing: a rival there lets a positional signal name
 * a winner in every clone family, which is what [isRealEvidence] exists to prevent.
 */
private fun strongestRivalScore(
    candidate: MatchCandidate,
    baselinesByReport: IdentityHashMap<Result, MutableList<Result>>,
    scoreByReportBaseline: IdentityHashMap<Result, IdentityHashMap<Result, MatchScore>>,
    state: DiffState,
): MatchScore? {
    // Any pair touching a collided endpoint is collided too, hence scored: the lookups below cannot miss.
    var strongest: MatchScore? = null
    val scoresForReport = scoreByReportBaseline.getValue(candidate.reportResult)
    for (baseline in baselinesByReport.getValue(candidate.reportResult)) {
        if (baseline === candidate.baselineResult || baseline.uniqueResultIndicator() !in state.undecidedFromBaseline) continue
        val score = scoresForReport.getValue(baseline)
        if (strongest == null || score > strongest) strongest = score
    }
    return strongest
}

/**
 * Whether the pair's context-snippet similarity reaches [floor]. Passes when a side has no (or a blank) snippet:
 * an unknown similarity is not a low one, so a producer without `contextRegion` gets no floor at all.
 */
private fun ResultFeatures.meetsSimilarityFloor(baseline: ResultFeatures, floor: Double): Boolean =
    contextSnippetLines == null || baseline.contextSnippetLines == null ||
            TiebreakerCascade.contextSnippetSimilarity(this, baseline) >= floor - PRECISION

/**
 * Whether far more report results compete for the baseline this pair takes than its report result has baselines to
 * choose from. Both counts are this pair's own contention, after earlier phases and the floor took what they took.
 *
 * Balanced means two revisions of the same code, whose problems mostly do correspond. Unbalanced means a clone
 * family - fixture trees, generated files - where most report results have no counterpart and all fit equally well.
 */
private fun isPoolUnbalanced(
    candidate: MatchCandidate,
    baselinesByReport: IdentityHashMap<Result, MutableList<Result>>,
    reportsPerBaseline: IdentityHashMap<Result, Int>,
): Boolean =
    reportsPerBaseline.getValue(candidate.baselineResult) >
            baselinesByReport.getValue(candidate.reportResult).size * UNBALANCED_POOL_RATIO

/**
 * Whether [decidingSignal] names a real difference: an [Signal.isIdentity] one, except [Signal.SNIPPET] on a bare
 * identifier. `null` (no rival) and [Signal.FALLBACK] (nothing told them apart) are not evidence.
 */
private fun isRealEvidence(decidingSignal: Signal?, reportFeatures: ResultFeatures): Boolean =
    decidingSignal?.isIdentity == true && (decidingSignal != Signal.SNIPPET || reportFeatures.hasSpecificSnippet)
