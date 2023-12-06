package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.Run
import com.jetbrains.qodana.sarif.model.VersionedMap

private typealias Fingerprint = String
private typealias FingerprintIndex = MultiMap<Fingerprint, Result>
private typealias KeyIndex = MultiMap<ResultKey, Result>

internal class RunResultGroup(
    private val baselineCalculation: BaselineCalculation,
    private val report: Run,
    baseline: Run
) {
    private val baselineHashes: FingerprintIndex
    private val reportHashes: FingerprintIndex
    private val diffBaseline: KeyIndex
    private val diffReport: KeyIndex
    private val reportLookup = DescriptorLookup(report)
    private val baselineLookup = DescriptorLookup(baseline)


    init {
        removeProblemsWithState(report, BaselineState.ABSENT)
        val (rHash, rDiff) = createIndices(report) { listOf(it.getLastValue(BaselineCalculation.EQUAL_INDICATOR)) }
        val (bHash, bDiff) = createIndices(baseline) { listOf(it.getLastValue(BaselineCalculation.EQUAL_INDICATOR)) }
        reportHashes = rHash
        diffReport = rDiff
        baselineHashes = bHash
        diffBaseline = bDiff
    }

    private fun createIndices(
        run: Run,
        selectPrints: (VersionedMap<String>) -> List<Fingerprint?>
    ): Pair<FingerprintIndex, KeyIndex> {
        val printIndex = FingerprintIndex()
        val keyIndex = KeyIndex()
        run.results.noNulls()
            .filterNot { it.baselineState == BaselineState.ABSENT }
            .forEach { result ->
                result.partialFingerprints?.let(selectPrints)
                    ?.forEach { print -> if (print != null) printIndex.add(print, result) }
                    ?: keyIndex.add(ResultKey(result), result)
            }

        return printIndex to keyIndex
    }


    private fun removeProblemsWithState(report: Run, state: BaselineState) {
        report.results.removeIf { result: Result -> result.baselineState == state }
    }

    fun build() {
        reportHashes.forEach { (fingerprint, results) ->
            if (baselineHashes.containsKey(fingerprint)) {
                results.forEach { it.baselineState = BaselineState.UNCHANGED }
                baselineCalculation.unchangedResults += results.size
            } else {
                results.forEach { diffReport.add(ResultKey(it), it) }
            }
        }

        baselineHashes.forEach { (fingerprint, results) ->
            if (!reportHashes.containsKey(fingerprint)) {
                for (result in results) {
                    if (baselineCalculation.options.wasChecked.apply(result)) {
                        diffBaseline.add(ResultKey(result), result)
                    } else {
                        result.baselineState = BaselineState.UNCHANGED
                        report.results.add(result)
                        baselineCalculation.unchangedResults += 1
                    }
                }
            }
        }

        diffReport.forEach { (key, results) ->
            val baselineDiffBucket = diffBaseline.getOrEmpty(key)
            for (result in results) {
                if (baselineDiffBucket.isEmpty()) {
                    result.baselineState = BaselineState.NEW
                    baselineCalculation.newResults++
                } else {
                    result.baselineState = BaselineState.UNCHANGED
                    baselineDiffBucket.removeAt(baselineDiffBucket.size - 1)
                    baselineCalculation.unchangedResults++
                }
            }
        }

        diffBaseline.asSequence()
            .flatMap { (_, v) -> v }
            .forEach { result ->
                if (baselineCalculation.options.wasChecked.apply(result)) {
                    if (baselineCalculation.options.includeAbsent) {
                        result.baselineState = BaselineState.ABSENT
                        baselineCalculation.absentResults++
                        report.results.add(result)
                        if (reportLookup.findById(result.ruleId) == null) {
                            val descriptor = baselineLookup.findById(result.ruleId)
                            descriptor?.addTo(report)
                        }
                    }
                } else {
                    result.baselineState = BaselineState.UNCHANGED
                    report.results.add(result)
                    baselineCalculation.unchangedResults += 1
                }
            }

        if (!baselineCalculation.options.includeUnchanged) {
            removeProblemsWithState(report, BaselineState.UNCHANGED)
            baselineCalculation.unchangedResults = 0
        }

        if (!baselineCalculation.options.fillBaselineState) {
            for (result in report.results) {
                result.baselineState = null
            }
        }
    }

    private fun <T : Any> Iterable<T?>?.noNulls(): Sequence<T> =
        this?.asSequence().orEmpty().filterNotNull()
}
