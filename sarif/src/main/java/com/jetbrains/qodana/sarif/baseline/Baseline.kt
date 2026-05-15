package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.CASCADE_HASHES
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run

private fun <T : Any> Iterable<T?>?.noNulls(): Sequence<T> =
    this?.asSequence().orEmpty().filterNotNull()

internal class DiffState(private val options: Options) {
    var new = 0
        private set
    var unchanged = 0
        private set
    var absent = 0
        private set

    val results = mutableListOf<Result>()

    fun put(result: Result, state: BaselineState, matchedBy: String? = null): Boolean {
        if (state == BaselineState.UNCHANGED && !options.includeUnchanged) return false
        if (state == BaselineState.ABSENT && !options.includeAbsent) return false

        if (matchedBy != null) result.updateProperties { it["matchedBy"] = matchedBy }
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

    val reportResults = report.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .toList()

    val undecidedFromReport = IdentitySet<Result>(reportResults.size).apply { addAll(reportResults) }

    val matchers: List<BaselineMatcher> = listOf(
        HashCascadeMatcher(reportResults, listOf(EQUAL_INDICATOR)),
        ResultKeyMatcher(reportResults),
        HashCascadeMatcher(reportResults, CASCADE_HASHES) { key, resolvedBy ->
            // Refuse same ast shape only matches that only positional tiebreakers could resolve — high FP risk.
            key != SAME_SHAPE || resolvedBy !in setOf("lineDelta", "fallback")
        }
    )

    var undecidedFromBaseline: List<Result> = baseline.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .toList()

    for (matcher in matchers) {
        val remaining = ArrayList<Result>(undecidedFromBaseline.size)
        for (b in undecidedFromBaseline) {
            val match = matcher.findMatch(b, undecidedFromReport)
            if (match != null) {
                undecidedFromReport.remove(match.result)
                state.put(match.result, BaselineState.UNCHANGED, match.matchedBy)
            } else {
                remaining.add(b)
            }
        }
        undecidedFromBaseline = remaining
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
