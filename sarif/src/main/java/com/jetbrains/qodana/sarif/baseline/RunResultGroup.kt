package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run
import com.jetbrains.qodana.sarif.model.VersionedMap

private typealias Fingerprint = String
private typealias StrictIndex = MultiMap<Fingerprint, Result>
private typealias LaxIndex = MultiMap<ResultKey, Result>

internal class RunResultGroup(
    private val baselineCalculation: BaselineCalculation,
    private val report: Run,
    private val baseline: Run
) {
    private val reportLookup = DescriptorLookup(report)
    private val baselineLookup = DescriptorLookup(baseline)

    private fun createIndices(
        run: Run,
        selectPrints: (VersionedMap<String>) -> Collection<Fingerprint?>
    ): Pair<StrictIndex, LaxIndex> {
        val printIndex = StrictIndex()
        val keyIndex = LaxIndex()
        run.results.noNulls()
            .filterNot { it.baselineState == BaselineState.ABSENT }
            .forEach { result ->
                result.partialFingerprints?.let(selectPrints)
                    ?.forEach { print -> if (print != null) printIndex.add(print, result) }
                    ?: keyIndex.add(ResultKey(result), result)
            }

        return printIndex to keyIndex
    }

    private fun Iterable<Result>.withBaselineState(state: BaselineState) =
        if (baselineCalculation.options.fillBaselineState) onEach { it.baselineState = state } else this

    private fun wasChecked(result: Result) = baselineCalculation.options.wasChecked.apply(result)

    private inline fun <T> MutableIterable<T>.each(crossinline f: MutableIterator<T>.(T) -> Unit) =
        with(iterator()) { forEachRemaining { f(it) } }

    fun build() {
        // shallow copies, to not mess with the underlying reports
        val undecidedFromReport = report.results.noNulls()
            .filterNotTo(mutableSetOf()) { it.baselineState == BaselineState.ABSENT }
        val undecidedFromBaseline = baseline.results.noNulls()
            .filterNotTo(mutableSetOf()) { it.baselineState == BaselineState.ABSENT }

        val (strictReportIndex, lossyReportIndex) = createIndices(report) {
            it.getValues(BaselineCalculation.EQUAL_INDICATOR).values
        }
        val (strictBaselineIndex, lossyBaselineIndex) = createIndices(baseline) {
            listOf(it.getLastValue(BaselineCalculation.EQUAL_INDICATOR))
        }

        val unchanged = mutableSetOf<Result>()
        val notChecked = mutableSetOf<Result>()
        val added = mutableSetOf<Result>()
        val absent = mutableSetOf<Result>()

        undecidedFromBaseline.each { result ->
            val foundInReport = result.fingerprints?.getValues(BaselineCalculation.EQUAL_INDICATOR)
                .orEmpty()
                .asSequence()
                .flatMap { (_, print) -> strictReportIndex.getOrEmpty(print) }
                .onEach(undecidedFromReport::remove)
                .onEach(unchanged::add)
                .count() != 0

            when {
                foundInReport -> remove()
                !wasChecked(result) -> {
                    notChecked.add(result)
                    remove()
                }
            }
        }

        undecidedFromReport.each { result ->
            val inBaseline = lossyBaselineIndex.getOrEmpty(ResultKey(result))
            if (inBaseline.isEmpty()) {
                added.add(result)
            } else {
                unchanged.add(result)
                undecidedFromBaseline.remove(inBaseline.removeFirst())
            }
        }

        undecidedFromBaseline.each { result ->
            if (wasChecked(result)) {
                absent.add(result)
            } else {
                unchanged.add(result)
            }
        }

        if (baselineCalculation.options.includeAbsent) {
            absent.asSequence()
                .filter { reportLookup.findById(it.ruleId) == null }
                .mapNotNull { baselineLookup.findById(it.ruleId) }
                .forEach { it.addTo(report) }
        }

        val theDiff = mutableListOf<Result>().apply {
            addAll(added.withBaselineState(BaselineState.NEW))
            baselineCalculation.newResults += added.size
            if (baselineCalculation.options.includeUnchanged) {
                addAll(unchanged.withBaselineState(BaselineState.UNCHANGED))
                addAll(notChecked.withBaselineState(BaselineState.UNCHANGED))
                baselineCalculation.unchangedResults += unchanged.size + notChecked.size
            }
            if (baselineCalculation.options.includeAbsent) {
                addAll(absent.withBaselineState(BaselineState.ABSENT))
                baselineCalculation.absentResults += absent.size
            }
        }

        report.withResults(theDiff)
    }

    private fun <T : Any> Iterable<T?>?.noNulls(): Sequence<T> =
        this?.asSequence().orEmpty().filterNotNull()
}
