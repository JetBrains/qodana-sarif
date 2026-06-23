package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_FUNC_AND_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_LOCATION_AND_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run
import java.util.IdentityHashMap

/** Non-null results that are still in play (not already marked absent). */
private fun Run.undecidedResults(): List<Result> =
    results.orEmpty().filterNotNull().filterNot { it.baselineState == BaselineState.ABSENT }

private fun funcNameAvailable(result: Result): Boolean {
    val fingerprints = result.partialFingerprints ?: return true
    val func = fingerprints.getLastValue(SAME_FUNC_AND_SHAPE) ?: return true
    val shape = fingerprints.getLastValue(SAME_SHAPE) ?: return true
    return func != shape
}

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
}

/** CAUTION: This mutates results in report and baseline **/
internal fun applyBaseline(report: Run, baseline: Run, options: Options): DiffState {
    val state = DiffState(options)
    val reportDescriptors = DescriptorLookup(report)
    val baselineDescriptors = DescriptorLookup(baseline)

    var undecidedFromReport: List<Result> = report.undecidedResults()
    val baselineResults = baseline.undecidedResults()

    // Baseline problems are the candidate pool
    val undecidedFromBaseline = IdentitySet<Result>(baselineResults.size).apply { addAll(baselineResults) }

    // equalIndicator is a unique, collision-free key: no scoring or tiebreaking
    undecidedFromReport = matchEqualIndicatorPhase(HashMatcher(baselineResults, EQUAL_INDICATOR), undecidedFromReport, undecidedFromBaseline, state)

    // The remaining keys can collide on a single result, so assign them globally by match quality
    val matchers: List<BaselineMatcher> = listOf(
        ResultKeyMatcher(baselineResults),
        HashMatcher(baselineResults, SAME_LOCATION_AND_SHAPE),
        HashMatcher(baselineResults, SAME_FUNC_AND_SHAPE, eligible = ::funcNameAvailable),
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
): List<Result> {
    val matchedReportProblems = IdentitySet<Result>(undecidedFromReport.size)
    for ((report, baseline, baseLabel, matchedWith) in matcher.candidates(undecidedFromReport, undecidedFromBaseline)) {
        if (report in matchedReportProblems || baseline !in undecidedFromBaseline) continue
        state.put(report, BaselineState.UNCHANGED, baseLabel, matchedWith)
        matchedReportProblems.add(report)
        undecidedFromBaseline.remove(baseline)
    }
    return undecidedFromReport.filterNot { it in matchedReportProblems }
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

    val matchedReportProblems = IdentitySet<Result>(candidates.size)
    val ranked = candidates
        .map { it to TiebreakerCascade.score(it.report, it.baseline) }
        .sortedByDescending { it.second }
    for ((candidate, _) in ranked) {
        val (report, baseline, baseLabel, matchedWith) = candidate
        if (report in matchedReportProblems || baseline !in undecidedFromBaseline) continue
        val matchedBy = baseLabel + tiebreakerSuffix(report, baseline, competitors.getValue(report))
        state.put(report, BaselineState.UNCHANGED, matchedBy, matchedWith)
        matchedReportProblems.add(report)
        undecidedFromBaseline.remove(baseline)
    }

    return undecidedFromReport.filterNot { it in matchedReportProblems }
}

private fun tiebreakerSuffix(report: Result, baseline: Result, competitors: List<Result>): String {
    if (competitors.size <= 1) return ""
    val resolution = TiebreakerCascade.resolve(report, competitors)
    return if (resolution?.result === baseline) "+${resolution.resolvedBy}" else ""
}
