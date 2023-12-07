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

internal data class BaselineSummary(val added: Int, val unchanged: Int, val absent: Int)

/** CAUTION: This mutates results in report and baseline **/
internal fun applyBaseline(report: Run, baseline: Run, options: Options): BaselineSummary {
    fun Iterable<Result>.withBaselineState(state: BaselineState) =
        if (options.fillBaselineState) onEach { it.baselineState = state } else this

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

    val unchanged = mutableSetOf<Result>()
    val notChecked = mutableSetOf<Result>()
    val added = mutableSetOf<Result>()
    val absent = mutableSetOf<Result>()

    undecidedFromBaseline.each { result ->
        val foundInReport = result.partialFingerprints?.getValues(BaselineCalculation.EQUAL_INDICATOR)
            .orEmpty()
            .asSequence()
            .flatMap { (_, print) -> reportIndex.getOrEmpty(print) }
            .onEach(undecidedFromReport::remove)
            .onEach(unchanged::add)
            .count() != 0

        when {
            foundInReport -> remove()
            !options.wasChecked.apply(result) -> {
                notChecked.add(result)
                remove()
            }
        }
    }

    undecidedFromReport.each { result ->
        val inBaseline = baselineIndex.getOrEmpty(ResultKey(result))
        if (inBaseline.isEmpty()) {
            added.add(result)
        } else {
            unchanged.add(result)
            undecidedFromBaseline.remove(inBaseline.removeFirst())
        }
    }

    undecidedFromBaseline.each { result ->
        if (options.wasChecked.apply(result)) {
            absent.add(result)
        } else {
            unchanged.add(result)
        }
    }

    if (options.includeAbsent) {
        absent.asSequence()
            .filter { reportDescriptors.findById(it.ruleId) == null }
            .mapNotNull { baselineDescriptors.findById(it.ruleId) }
            .forEach { it.addTo(report) }
    }

    val theDiff = mutableListOf<Result>().apply {
        addAll(added.withBaselineState(BaselineState.NEW))
        if (options.includeUnchanged) {
            addAll(unchanged.withBaselineState(BaselineState.UNCHANGED))
            addAll(notChecked.withBaselineState(BaselineState.UNCHANGED))
        }
        if (options.includeAbsent) {
            addAll(absent.withBaselineState(BaselineState.ABSENT))
        }
    }

    report.withResults(theDiff)

    return BaselineSummary(
        added = added.size,
        unchanged = if (options.includeUnchanged) unchanged.size + notChecked.size else 0,
        absent = if (options.includeAbsent) absent.size else 0
    )
}
