package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run

private typealias Fingerprint = String
private typealias StrictIndex = MultiMap<Fingerprint, Result>
private typealias LaxIndex = MultiMap<ResultKey, Result>

private inline fun <T> MutableIterable<T>.each(crossinline f: MutableIterator<T>.(T) -> Unit) =
    with(iterator()) { forEachRemaining { f(it) } }

private fun <T : Any> Iterable<T?>?.noNulls(): Sequence<T> =
    this?.asSequence().orEmpty().filterNotNull()

internal class DiffState(private val options: Options) {
    var new = 0
        private set
    var unchanged = 0
        private set
    var absent = 0
        private set

    val results = mutableSetOf<Result>()

    fun put(result: Result, state: BaselineState): Boolean {
        if (state == BaselineState.UNCHANGED && !options.includeUnchanged) return false
        if (state == BaselineState.ABSENT && !options.includeAbsent) return false
        val added = results.add(
            result.withBaselineState(if (options.fillBaselineState) state else null)
        )
        if (added) {
            when (state) {
                BaselineState.NEW -> new++
                BaselineState.UNCHANGED -> unchanged++
                BaselineState.ABSENT -> absent++
                BaselineState.UPDATED -> Unit
            }
        }
        return added
    }
}

/** CAUTION: This mutates results in report and baseline **/
internal fun applyBaseline(report: Run, baseline: Run, options: Options): DiffState {
    val reportDescriptors = DescriptorLookup(report)
    val baselineDescriptors = DescriptorLookup(baseline)
    val reportIndex = StrictIndex()
    val baselineIndex = LaxIndex()

    // shallow copies, to not mess with the underlying reports
    val undecidedFromReport = report.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .onEach { result ->
            result.partialFingerprints
                ?.getValues(BaselineCalculation.EQUAL_INDICATOR)
                ?.values
                .noNulls()
                .forEach { print -> reportIndex.add(print, result) }
        }
        .toMutableSet()

    val undecidedFromBaseline = baseline.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .onEach { result -> baselineIndex.add(ResultKey(result), result) }
        .toMutableSet()

    val state = DiffState(options)

    undecidedFromBaseline.each { result ->
        val foundInReport = result.partialFingerprints?.getValues(BaselineCalculation.EQUAL_INDICATOR)
            .orEmpty()
            .asSequence()
            .flatMap { (_, print) -> reportIndex.getOrEmpty(print) }
            .onEach(undecidedFromReport::remove)
            .onEach { state.put(it, BaselineState.UNCHANGED) }
            .count() != 0

        when {
            foundInReport -> remove()
            !options.wasChecked.apply(result) -> {
                remove()
                state.put(result, BaselineState.UNCHANGED)
            }
        }
    }

    undecidedFromReport.each { result ->
        val inBaseline = baselineIndex.getOrEmpty(ResultKey(result))
        if (inBaseline.isEmpty()) {
            state.put(result, BaselineState.NEW)
        } else {
            undecidedFromBaseline.remove(inBaseline.removeFirst())
            state.put(result, BaselineState.UNCHANGED)
        }
    }

    undecidedFromBaseline.each { result ->
        if (!options.wasChecked.apply(result)) {
            state.put(result, BaselineState.UNCHANGED)
            return@each
        }
        if (state.put(result, BaselineState.ABSENT) && reportDescriptors.findById(result.ruleId) == null) {
            baselineDescriptors.findById(result.ruleId)?.addTo(report)
        }
    }

    report.withResults(state.results.toMutableList())

    return state
}
