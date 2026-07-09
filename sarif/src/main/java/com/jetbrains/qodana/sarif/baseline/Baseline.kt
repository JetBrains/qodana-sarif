package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SHIFT_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.MOVE_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run
import java.util.IdentityHashMap

/** Non-null results that are not already marked absent. */
private fun Run.undecidedResults(): List<Result> =
    results.orEmpty().mapNotNull { result -> result?.takeIf { it.baselineState != BaselineState.ABSENT } }

/** The matched baseline's equalIndicator (latest version), recorded on the report result as `matchedWith`. */
private fun Result.equalIndicator(): String =
    partialFingerprints?.getLastValue(EQUAL_INDICATOR) ?: ""

private fun Result.equalIndicatorV1(): String {
    val fingerprints = partialFingerprints ?: return ""
    return fingerprints.get(EQUAL_INDICATOR, 1) ?: fingerprints.getLastValue(EQUAL_INDICATOR) ?: ""
}

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

    private val includeMatchedBy = System.getProperty(BaselineCalculation.INCLUDE_MATCHED_BY_PROPERTY) == "true"

    fun put(result: Result, state: BaselineState, matchedBy: String? = null, matchedWith: String? = null): Boolean {
        if (state == BaselineState.UNCHANGED && !options.includeUnchanged) return false
        if (state == BaselineState.ABSENT && !options.includeAbsent) return false

        if (includeMatchedBy && matchedBy != null) result.updateProperties { it["matchedBy"] = matchedBy }
        if (matchedWith != null) result.updateProperties { it["matchedWith"] = matchedWith }
        results.add(result.withBaselineState(if (options.fillBaselineState) state else null))
        when (state) {
            BaselineState.NEW -> new++
            BaselineState.UNCHANGED -> unchanged++
            BaselineState.ABSENT -> absent++
            BaselineState.UPDATED -> Unit
        }
        return true
    }

    fun isMatchedByIncluded(): Boolean = includeMatchedBy

    /** Records an UNCHANGED match and consumes both endpoints (by their equalIndicator id) from the candidate pools. */
    fun commit(reportResult: Result, baselineResult: Result, matchedBy: String) {
        put(reportResult, BaselineState.UNCHANGED, matchedBy, baselineResult.equalIndicator())
        undecidedFromReport.remove(reportResult.equalIndicatorV1())
        undecidedFromBaseline.remove(baselineResult.equalIndicatorV1())
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
        reportResults.associateByTo(LinkedHashMap()) { it.equalIndicatorV1() },
        baselineResults.associateByTo(LinkedHashMap()) { it.equalIndicatorV1() },
    )

    // equalIndicator is a unique, collision-free key: no scoring or tiebreaking. The remaining hashes can collide
    // on a single result, so each later phase assigns its still-undecided results globally by match quality.
    matchEqualIndicatorPhase(state)
    for (fingerprintKey in listOf(SHIFT_TOLERANT_INDICATOR, MOVE_AND_REFACTOR_TOLERANT_INDICATOR, EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR)) {
        matchPhase(fingerprintKey, state)
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
    for ((reportResult, baselineResult, matchedBy) in matcher.candidates(state.undecidedFromReport.values)) {
        if (reportResult.equalIndicatorV1() in state.undecidedFromReport && baselineResult.equalIndicatorV1() in state.undecidedFromBaseline) {
            state.commit(reportResult, baselineResult, matchedBy)
        }
    }
}

/**
 * Resolves one matching phase for a key that can collide on a single result.
 *
 * 1:1 pairs are committed directly. The rest are assigned by global greedy-best-first: every contested pairing is
 * scored, then committed strongest-score-first so a weaker pair can never steal a baseline from a stronger one.
 * Matched endpoints are removed from [state]'s pools in place, so the leftovers flow to the next phase.
 */
private fun matchPhase(fingerprintKey: String, state: DiffState) {
    val matcher = HashMatcher(state.undecidedFromBaseline.values, fingerprintKey)
    val candidates = matcher.candidates(state.undecidedFromReport.values)
    if (candidates.isEmpty()) return

    val baselinesByReport = IdentityHashMap<Result, MutableList<Result>>()
    val reportsPerBaseline = IdentityHashMap<Result, Int>()
    for ((reportResult, baselineResult) in candidates) {
        baselinesByReport.getOrPut(reportResult) { ArrayList() }.add(baselineResult)
        reportsPerBaseline.merge(baselineResult, 1, Int::plus)
    }

    val candidatesWithCollisions = ArrayList<MatchCandidate>(candidates.size)
    for (c in candidates) {
        if (baselinesByReport.getValue(c.reportResult).size == 1 && reportsPerBaseline.getValue(c.baselineResult) == 1) {
            state.commit(c.reportResult, c.baselineResult, c.matchedBy)
        } else {
            candidatesWithCollisions.add(c)
        }
    }
    if (candidatesWithCollisions.isEmpty()) return

    val featuresOf = ResultFeatures.Cache()

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
            if (candidate.reportResult.equalIndicatorV1() !in state.undecidedFromReport || candidate.baselineResult.equalIndicatorV1() !in state.undecidedFromBaseline) return@forEach
            val suffix = if (state.isMatchedByIncluded()) {
                tiebreakerSuffix(candidate, score, baselinesByReport, scoreByReportBaseline, state)
            } else {
                ""
            }
            state.commit(candidate.reportResult, candidate.baselineResult, candidate.matchedBy + suffix)
        }
}

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
        .filter { it !== candidate.baselineResult && it.equalIndicatorV1() in state.undecidedFromBaseline }
        .maxOfOrNull { scores.getValue(it) }
    return committedScore.decidingSignalAgainst(rivalScore)?.let { "+$it" } ?: ""
}
