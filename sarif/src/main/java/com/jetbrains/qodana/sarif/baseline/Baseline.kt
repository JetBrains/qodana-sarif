package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run

private typealias FingerprintIndex = MultiMap<String, Result>

private fun <T : Any> Iterable<T?>?.noNulls(): Sequence<T> =
    this?.asSequence().orEmpty().filterNotNull()

private val Result.equalIndicators: Sequence<String>
    get() = partialFingerprints?.getValues(BaselineCalculation.EQUAL_INDICATOR)?.entries
        .noNulls()
        .map { (k, v) -> "$k:$v" }
        .sortedDescending() // higher versions should have higher priority

internal class DiffState(private val options: Options) {
    var new = 0
        private set
    var unchanged = 0
        private set
    var absent = 0
        private set

    val results = mutableListOf<Result>()

    fun put(result: Result, state: BaselineState): Boolean {
        if (state == BaselineState.UNCHANGED && !options.includeUnchanged) return false
        if (state == BaselineState.ABSENT && !options.includeAbsent) return false

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
    val reportIndex = FingerprintIndex()
    val baselineCounter = Counter<ResultKey>()

    // filling reportIndex with all results form report
    val undecidedFromReport = report.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .onEach { result ->
            result.equalIndicators
                .forEach { print -> reportIndex.add(print, result) }
        }
        .toCollection(IdentitySet(report.results?.size ?: 0))

    val filteredResults = baseline.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }

    // filling baselineCounter
    filteredResults
        .forEach { result -> baselineCounter.increment(ResultKey(result)) }

    // removing from baseline report results matched by fingerprints with report
    val undecidedFromBaseline = buildList {
        filteredResults
            .forEach { result ->
                //compare with all equal indicators
                val matchedResults = result.equalIndicators.flatMap(reportIndex::getOrEmpty).toSet()
                var removed = false
                for (matchedResult in matchedResults) {
                    removed = removed || undecidedFromReport.remove(matchedResult)
                }

                if (removed) {
                    //leads to eliminating problems with the same hash
                    state.put(matchedResults.first(), BaselineState.UNCHANGED)
                    remove(matchedResults.first())
                    baselineCounter.decrement(ResultKey(matchedResults.first()))
                } else {
                    if (!options.wasChecked.apply(result)) {
                        state.put(result, BaselineState.UNCHANGED)
                    } else {
                        add(result)
                    }
                }
            }
    }

    // at this point in undecidedFromReport problems not matched with baseline using fingerprints
    // separating these problems on NEW and UNCHANGED using comparison from ResultKey
    undecidedFromReport.forEach { result ->
        val key = ResultKey(result)
        val inBaseline = baselineCounter[key]
        if (inBaseline <= 0) {
            state.put(result, BaselineState.NEW)
        } else {
            baselineCounter.decrement(key)
            state.put(result, BaselineState.UNCHANGED)
        }
    }

    // adding to ABSENT problems from baseline not matched by fingerprints or ResultKey comparison
    undecidedFromBaseline
        .asSequence()
        .filter { baselineCounter.decrement(ResultKey(it)) >= 0 }
        .forEach { result ->
            val added = state.put(result, BaselineState.ABSENT)
            if (added && reportDescriptors.findById(result.ruleId) == null) {
                baselineDescriptors.findById(result.ruleId)?.addTo(report)
            }
        }

    report.withResults(state.results)

    return state
}
