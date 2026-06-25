package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_FUNC_AND_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_LOCATION_AND_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SHIFT_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.MOVE_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run
import java.util.IdentityHashMap

/** Non-null results that are still in play (not already marked absent). */
private fun Run.undecidedResults(): List<Result> =
    results.orEmpty().filterNotNull().filterNot { it.baselineState == BaselineState.ABSENT }

internal class DiffState(private val options: Options) {
    var new = 0
        private set
    var unchanged = 0
        private set
    var absent = 0
        private set

    val results = mutableListOf<Result>()

    fun put(result: Result, state: BaselineState, matchedBy: String? = null, matchedWith: String? = null): Boolean {
        if (state == BaselineState.UNCHANGED && !options.includeUnchanged) return false
        if (state == BaselineState.ABSENT && !options.includeAbsent) return false

        if (matchedBy != null) result.updateProperties { it["matchedBy"] = matchedBy }
        if (options.includeMatchedBy && matchedWith != null) result.updateProperties { it["matchedWith"] = matchedWith }
        results.add(result.withBaselineState(if (options.fillBaselineState) state else null))
        when (state) {
            BaselineState.NEW -> new++
            BaselineState.UNCHANGED -> unchanged++
            BaselineState.ABSENT -> absent++
            BaselineState.UPDATED -> Unit
        }
        return true
    }
}

/** CAUTION: This mutates results in report and baseline **/
internal fun applyBaseline(report: Run, baseline: Run, options: Options): DiffState {
    val state = DiffState(options)
    val reportDescriptors = DescriptorLookup(report)
    val baselineDescriptors = DescriptorLookup(baseline)

    var undecidedFromReport: List<Result> = report.undecidedResults()
    val baselineResults = baseline.undecidedResults()

    // Baseline problems are the candidate pool
    var undecidedFromBaseline = IdentitySet<Result>(baselineResults.size).apply { addAll(baselineResults) }

    //TODO: to remove
    undecidedFromBaseline.find { it.partialFingerprints?.getLastValue(SAME_LOCATION_AND_SHAPE)?.isNotEmpty() ?: false } ?: return applyBaselineOldAlg(report, baseline, options, state)

    // equalIndicator is a unique, collision-free key: no scoring or tiebreaking
    matchEqualIndicatorPhase(HashMatcher(baselineResults, EQUAL_INDICATOR), undecidedFromReport, undecidedFromBaseline, state).let { (remainingFromReport, remainingFromBaseline) ->
        undecidedFromReport = remainingFromReport
        undecidedFromBaseline = remainingFromBaseline
    }

    // The remaining keys can collide on a single result, so assign them globally by match quality
    //TODO: update matchers
    val matchers: List<BaselineMatcher> = listOf(
        //HashMatcher(baselineResults, SHIFT_TOLERANT_INDICATOR),
        //HashMatcher(baselineResults, MOVE_AND_REFACTOR_TOLERANT_INDICATOR),
        //HashMatcher(baselineResults, EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR),
        //ResultKeyMatcher(baselineResults),
        ResultKeyMatcher(baselineResults),
        HashMatcher(baselineResults, SAME_LOCATION_AND_SHAPE),
        HashMatcher(baselineResults, SAME_FUNC_AND_SHAPE),
    )
    for (matcher in matchers) {
        undecidedFromReport = matchPhase(matcher, undecidedFromReport, undecidedFromBaseline, state)
    }

    undecidedFromReport.forEach { state.put(it, BaselineState.NEW) }
    undecidedFromBaseline.forEach { result ->
        if (!options.wasChecked.apply(result)) {
            state.put(result, BaselineState.UNCHANGED)
        } else if (state.put(result, BaselineState.ABSENT) && reportDescriptors.findById(result.ruleId) == null) {
            baselineDescriptors.findById(result.ruleId)?.addTo(report)
        }
    }

    report.withResults(state.results)

    return state
}

/** Commits exact matches from a unique, collision-free key (equalIndicator) */
private fun matchEqualIndicatorPhase(
    matcher: BaselineMatcher,
    undecidedFromReport: List<Result>,
    undecidedFromBaseline: IdentitySet<Result>,
    state: DiffState,
): Pair<List<Result>, IdentitySet<Result>> {
    val decidedFromReport = IdentitySet<Result>(undecidedFromReport.size)
    for ((reportResult, baselineResult, matchedBy, matchedWith) in matcher.candidates(undecidedFromReport, undecidedFromBaseline)) {
        if (reportResult in decidedFromReport || baselineResult !in undecidedFromBaseline) continue
        state.put(reportResult, BaselineState.UNCHANGED, matchedBy, matchedWith)
        decidedFromReport.add(reportResult)
        undecidedFromBaseline.remove(baselineResult)
    }
    return (undecidedFromReport.filterNot { it in decidedFromReport } to undecidedFromBaseline)
}

/**
 * Resolves one matching phase by global greedy-best-first assignment, for keys that can collide on a
 * single result.
 *
 * Instead of committing the first acceptable match per problem, this collects every candidate match,
 * scores each pairing, then commits them strongest-score-first.
 *
 * Matched baselines are removed from [undecidedFromBaseline]; the returned list is the reports that
 * remain unmatched and flow to the next phase.
 */
private fun matchPhase(
    matcher: BaselineMatcher,
    undecidedFromReport: List<Result>,
    undecidedFromBaseline: IdentitySet<Result>,
    state: DiffState,
): List<Result> {
    val candidates = matcher.candidates(undecidedFromReport, undecidedFromBaseline)
    if (candidates.isEmpty()) return undecidedFromReport

    val competitors = IdentityHashMap<Result, MutableList<Result>>()
    for ((report, baseline) in candidates) competitors.getOrPut(report) { ArrayList() }.add(baseline)

    val decidedFromReport = IdentitySet<Result>(candidates.size)
    val ranked = candidates
        .map { it to TiebreakerCascade.score(it.reportResult, it.baselineResult) }
        .sortedByDescending { it.second }
    for ((candidate, _) in ranked) {
        val (reportResult, baselineResult, matchedFingerprintHash, matchedWith) = candidate
        if (reportResult in decidedFromReport || baselineResult !in undecidedFromBaseline) continue
        val matchedBy = matchedFingerprintHash + tiebreakerSuffix(reportResult, baselineResult, competitors.getValue(reportResult))
        state.put(reportResult, BaselineState.UNCHANGED, matchedBy, matchedWith)
        decidedFromReport.add(reportResult)
        undecidedFromBaseline.remove(baselineResult)
    }

    return undecidedFromReport.filterNot { it in decidedFromReport }
}

private fun tiebreakerSuffix(reportResult: Result, baselineResult: Result, competitors: List<Result>): String {
    if (competitors.size <= 1) return ""
    val resolution = TiebreakerCascade.resolve(reportResult, competitors)
    return if (resolution?.result === baselineResult) "+${resolution.resolvedBy}" else ""
}
