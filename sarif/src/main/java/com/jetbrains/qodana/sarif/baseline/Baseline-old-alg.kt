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

/** CAUTION: This mutates results in report and baseline **/
internal fun applyBaselineOldAlg(report: Run, baseline: Run, options: Options, state: DiffState): DiffState {
    val reportDescriptors = DescriptorLookup(report)
    val baselineDescriptors = DescriptorLookup(baseline)
    val reportIndex = FingerprintIndex()
    val baselineCounter = Counter<ResultKey>()

    val undecidedFromReport = report.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .onEach { result ->
            result.equalIndicators
                .forEach { print -> reportIndex.add(print, result) }
        }
        .toCollection(IdentitySet(report.results?.size ?: 0))

    val undecidedFromBaseline = buildList {
        baseline.results.noNulls()
            .filterNot { it.baselineState == BaselineState.ABSENT }
            .onEach { result -> baselineCounter.increment(ResultKey(result)) }
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
                } else {
                    if (!options.wasChecked.apply(result)) {
                        state.put(result, BaselineState.UNCHANGED)
                    } else {
                        add(result)
                    }
                }
            }
    }


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
