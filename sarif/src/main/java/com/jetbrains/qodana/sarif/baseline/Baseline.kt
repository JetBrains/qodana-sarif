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
    // Every key below can pair unrelated results, so a pair must clear its phase's similarity floor.
    for ((fingerprintKey, similarityFloor) in listOf(
        SHIFT_TOLERANT_INDICATOR to NO_SIMILARITY_FLOOR,
        MOVE_AND_REFACTOR_TOLERANT_INDICATOR to SIMILARITY_FLOOR,
        EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to SIMILARITY_FLOOR,
        SCOPE_AND_SHAPE_TOLERANT_INDICATOR to SIMILARITY_FLOOR,
    )) {
        matchPhase(fingerprintKey, state, similarityFloor)
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
    for ((reportResult, baselineResult, matchedMethod) in matcher.candidates(state.undecidedFromReport.values)) {
        if (reportResult.uniqueResultIndicator() in state.undecidedFromReport && baselineResult.uniqueResultIndicator() in state.undecidedFromBaseline) {
            state.commit(reportResult, baselineResult, matchedMethod)
        }
    }
}

/**
 * Resolves one matching phase for a key that can collide on a single result.
 *
 * 1:1 pairs are committed directly. The rest are assigned by global greedy-best-first: every contested pairing is
 * scored, then committed strongest-score-first so a weaker pair can never steal a baseline from a stronger one.
 * Matched endpoints are removed from [state]'s pools in place, so the leftovers flow to the next phase.
 *
 * Every pair is committed only if its own context-snippet similarity reaches [similarityFloor].
 */
private fun matchPhase(
    fingerprintKey: String,
    state: DiffState,
    similarityFloor: Double,
) {
    val matcher = HashMatcher(state.undecidedFromBaseline.values, fingerprintKey)
    val candidates = matcher.candidates(state.undecidedFromReport.values)
    if (candidates.isEmpty()) return

    val baselinesByReport = IdentityHashMap<Result, MutableList<Result>>()
    val reportsPerBaseline = IdentityHashMap<Result, Int>()
    for ((reportResult, baselineResult) in candidates) {
        baselinesByReport.getOrPut(reportResult) { ArrayList() }.add(baselineResult)
        reportsPerBaseline.merge(baselineResult, 1, Int::plus)
    }

    val featuresOf = ResultFeatures.Cache()
    val candidatesWithCollisions = ArrayList<MatchCandidate>(candidates.size)
    for (c in candidates) {
        if (baselinesByReport.getValue(c.reportResult).size != 1 || reportsPerBaseline.getValue(c.baselineResult) != 1) {
            candidatesWithCollisions.add(c)
        } else if (featuresOf(c.reportResult).meetsSimilarityFloor(featuresOf(c.baselineResult), similarityFloor)) {
            state.commit(c.reportResult, c.baselineResult, c.matchedMethod)
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
            if (candidate.reportResult.uniqueResultIndicator() !in state.undecidedFromReport || candidate.baselineResult.uniqueResultIndicator() !in state.undecidedFromBaseline) return@forEach
            if (!featuresOf(candidate.reportResult).meetsSimilarityFloor(featuresOf(candidate.baselineResult), similarityFloor)) return@forEach
            val suffix = if (state.isMatchedMethodIncluded()) {
                tiebreakerSuffix(candidate, score, baselinesByReport, scoreByReportBaseline, state)
            } else {
                ""
            }
            state.commit(candidate.reportResult, candidate.baselineResult, candidate.matchedMethod + suffix)
        }
}

/**
 * Whether the pair's own context-snippet similarity reaches [floor], so the pair may be committed. Passes when the
 * phase applies no floor, or when a side has no context snippet: an unknown similarity is not a low one.
 */
private fun ResultFeatures.meetsSimilarityFloor(baseline: ResultFeatures, floor: Double): Boolean =
    floor <= NO_SIMILARITY_FLOOR ||
            contextSnippetLines == null || baseline.contextSnippetLines == null ||
            TiebreakerCascade.contextSnippetSimilarity(this, baseline) >= floor

/**
 * The signal that won the committed pair against the strongest remaining rival.
 * Since matches commit strongest-first, this is always a decisive win (or `""` if no rivals remain).
 */
private fun tiebreakerSuffix(
    candidate: MatchCandidate,
    committedScore: MatchScore,
    baselinesByReport: IdentityHashMap<Result, MutableList<Result>>,
    scoreByReportBaseline: IdentityHashMap<Result, IdentityHashMap<Result, MatchScore>>,
    state: DiffState,
): String {
    val scores = scoreByReportBaseline.getValue(candidate.reportResult)
    val rivalScore = baselinesByReport.getValue(candidate.reportResult)
        .filter { it !== candidate.baselineResult && it.uniqueResultIndicator() in state.undecidedFromBaseline }
        .maxOfOrNull { scores.getValue(it) }
    return committedScore.decidingSignalAgainst(rivalScore)?.let { "+$it" } ?: ""
}
