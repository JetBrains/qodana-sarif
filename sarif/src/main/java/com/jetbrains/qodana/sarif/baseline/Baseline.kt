package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run

private typealias FingerprintIndex = MultiMap<String, Result>

private inline fun <T> MutableIterable<T>.each(crossinline f: MutableIterator<T>.(T) -> Unit) {
    val iter = iterator()
    // don't use `iter.forEachRemaining` as that is incompatible with `iter.remove`
    while (iter.hasNext()) {
        iter.f(iter.next())
    }
}

private fun <T : Any> Iterable<T?>?.noNulls(): Sequence<T> =
    this?.asSequence().orEmpty().filterNotNull()

private val Result.equalIndicators: Sequence<String>
    get() = partialFingerprints?.getValues(BaselineCalculation.EQUAL_INDICATOR)?.values.noNulls()

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

    val undecidedFromReport = report.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .onEach { result ->
            result.equalIndicators
                .forEach { print -> reportIndex.add(print, result) }
        }
        .toCollection(IdentitySet(report.results?.size ?: 0))

    val undecidedFromBaseline = baseline.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .onEach { result -> baselineCounter.increment(ResultKey(result)) }
        .filter { result ->
            val foundInReport = result.equalIndicators
                .flatMap(reportIndex::getOrEmpty)
                .filter(undecidedFromReport::remove)
                .onEach { state.put(it, BaselineState.UNCHANGED) }
                .firstOrNull() != null

            if (foundInReport) {
                return@filter false
            }
            if (!options.wasChecked.apply(result)) {
                state.put(result, BaselineState.UNCHANGED)
                return@filter false
            }
            true
        }
        .toMutableList()

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
            // Why do I need this here again?
            if (!options.wasChecked.apply(result)) {
                state.put(result, BaselineState.UNCHANGED)
                return@forEach
            }
            if (state.put(result, BaselineState.ABSENT) && reportDescriptors.findById(result.ruleId) == null) {
                baselineDescriptors.findById(result.ruleId)?.addTo(report)
            }
        }

    report.withResults(state.results)

    return state
}
