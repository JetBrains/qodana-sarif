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

/** The matched baseline's equalIndicator, recorded on the report result as `matchedWith`. */
private fun Result.equalIndicator(): String =
    partialFingerprints?.getLastValue(EQUAL_INDICATOR) ?: ""

/** New-analyzer reports carry at least one of these; their absence means a legacy baseline. */
private fun Result.hasHash(key: String): Boolean =
    partialFingerprints?.getLastValue(key)?.isNotEmpty() == true

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

        if (options.includeMatchedBy && matchedBy != null) result.updateProperties { it["matchedBy"] = matchedBy }
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

    // shiftTolerantEqualIndicator is the analyzer-generated 1:1 equivalent of ResultKey content equality hash
    val hasUseShiftTolerantHash = undecidedFromBaseline.asUnordered().any { it.hasHash(SHIFT_TOLERANT_INDICATOR) }
            || undecidedFromReport.any { it.hasHash(SHIFT_TOLERANT_INDICATOR) }

    //TODO: to remove before the release
    //--------------------------------------------------------------------//
    if (!hasUseShiftTolerantHash) return applyBaselineOldAlg(report, baseline, options, state)
    //--------------------------------------------------------------------//

    // equalIndicator is a unique, collision-free key: no scoring or tiebreaking
    undecidedFromReport = matchEqualIndicatorPhase(
        HashMatcher(undecidedFromBaseline.asUnordered(), EQUAL_INDICATOR), undecidedFromReport, undecidedFromBaseline, state,
    )

    // The remaining hashes can collide on a single result, so each phase indexes only the remaining baseline results
    // and assigns them globally by match quality.
    val matchers: List<(Set<Result>) -> BaselineMatcher> = listOf(
        { remaining -> if (hasUseShiftTolerantHash) HashMatcher(remaining, SHIFT_TOLERANT_INDICATOR) else ResultKeyMatcher(remaining) },
        { remaining -> HashMatcher(remaining, MOVE_AND_REFACTOR_TOLERANT_INDICATOR) },
        { remaining -> HashMatcher(remaining, EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR) },
    )
    for (matcher in matchers) {
        undecidedFromReport = matchPhase(matcher(undecidedFromBaseline.asUnordered()), undecidedFromReport, undecidedFromBaseline, state)
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

/**
 * Commits exact matches from a unique, collision-free key (equalIndicator). Matched baselines are removed from
 * [undecidedFromBaseline] in place; the returned list is the reports that remain unmatched.
 */
private fun matchEqualIndicatorPhase(
    matcher: BaselineMatcher,
    undecidedFromReport: List<Result>,
    undecidedFromBaseline: IdentitySet<Result>,
    state: DiffState,
): List<Result> {
    val decidedFromReport = IdentitySet<Result>(undecidedFromReport.size)
    for ((reportResult, baselineResult, matchedBy) in matcher.candidates(undecidedFromReport)) {
        if (reportResult in decidedFromReport || baselineResult !in undecidedFromBaseline) continue
        state.put(reportResult, BaselineState.UNCHANGED, matchedBy, baselineResult.equalIndicator())
        decidedFromReport.add(reportResult)
        undecidedFromBaseline.remove(baselineResult)
    }
    return undecidedFromReport.filterNot { it in decidedFromReport }
}

/**
 * Resolves one matching phase for keys that can collide on a single result.
 *
 * 1:1 pairs are commited directly. The rest are assigned by global greedy-best-first: every contested pairing is
 * scored, then committed strongest-score-first so a weaker pair can never steal a baseline from a stronger one.
 *
 * Matched baselines are removed from [undecidedFromBaseline]; the returned list is the reports that remain
 * unmatched and flow to the next phase.
 */
private fun matchPhase(
    matcher: BaselineMatcher,
    undecidedFromReport: List<Result>,
    undecidedFromBaseline: IdentitySet<Result>,
    state: DiffState,
): List<Result> {
    val candidates = matcher.candidates(undecidedFromReport)
    if (candidates.isEmpty()) return undecidedFromReport

    val baselinesByReport = IdentityHashMap<Result, MutableList<Result>>()
    val reportsPerBaseline = IdentityHashMap<Result, Int>()
    for ((reportResult, baselineResult) in candidates) {
        baselinesByReport.getOrPut(reportResult) { ArrayList() }.add(baselineResult)
        reportsPerBaseline.merge(baselineResult, 1, Int::plus)
    }

    val decidedFromReport = IdentitySet<Result>(candidates.size)
    fun commit(candidate: MatchCandidate, matchedBySuffix: String) {
        state.put(candidate.reportResult, BaselineState.UNCHANGED, candidate.matchedBy + matchedBySuffix, candidate.baselineResult.equalIndicator())
        decidedFromReport.add(candidate.reportResult)
        undecidedFromBaseline.remove(candidate.baselineResult)
    }

    val candidatesWithCollisions = ArrayList<MatchCandidate>(candidates.size)
    for (c in candidates) {
        if (baselinesByReport.getValue(c.reportResult).size == 1 && reportsPerBaseline.getValue(c.baselineResult) == 1) {
            commit(c, "")
        } else {
            candidatesWithCollisions.add(c)
        }
    }

    if (candidatesWithCollisions.isNotEmpty()) {
        val featureCache = IdentityHashMap<Result, ResultFeatures>()
        fun featuresOf(result: Result): ResultFeatures = featureCache.getOrPut(result) { ResultFeatures(result) }

        candidatesWithCollisions
            .map { it to TiebreakerCascade.score(featuresOf(it.reportResult), featuresOf(it.baselineResult)) }
            .sortedByDescending { it.second }
            .forEach { (candidate, _) ->
                if (candidate.reportResult in decidedFromReport || candidate.baselineResult !in undecidedFromBaseline) return@forEach
                val competitors = baselinesByReport.getValue(candidate.reportResult)
                commit(candidate, tiebreakerSuffix(featuresOf(candidate.reportResult), candidate.baselineResult, competitors, ::featuresOf))
            }
    }

    return undecidedFromReport.filterNot { it in decidedFromReport }
}

private fun tiebreakerSuffix(
    reportResultFeatures: ResultFeatures,
    baselineResult: Result,
    competitors: List<Result>,
    featuresOf: (Result) -> ResultFeatures,
): String {
    if (competitors.size <= 1) return ""
    val resolution = TiebreakerCascade.resolve(reportResultFeatures, competitors.map(featuresOf))
    return if (resolution?.result === baselineResult) "+${resolution.resolvedBy}" else ""
}
