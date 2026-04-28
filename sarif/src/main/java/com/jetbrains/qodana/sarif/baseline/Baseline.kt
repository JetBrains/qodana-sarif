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

private val Result.extendedIndicators: Sequence<String>
    get() = extendedFingerprints?.getValues(BaselineCalculation.EXTENDED_FINGERPRINT)?.entries
        .noNulls()
        .map { (k, v) -> "${BaselineCalculation.EXTENDED_FINGERPRINT}/v$k:$v" }
        .sortedDescending() // higher tiers should have higher priority

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
    val reportIndex = FingerprintIndex()
    val extendedReportIndex = FingerprintIndex()

    val undecidedFromReport = report.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .onEach { result ->
            result.equalIndicators.forEach { reportIndex.add(it, result) }
            result.extendedIndicators.forEach { extendedReportIndex.add(it, result) }
        }
        .toCollection(IdentitySet(report.results?.size ?: 0))

    val undecidedFromBaseline = mutableListOf<Result>()
    baseline.results.noNulls()
        .filterNot { it.baselineState == BaselineState.ABSENT }
        .forEach { result ->
            //compare with all equal indicators
            val matchedResults = result.equalIndicators.flatMap(reportIndex::getOrEmpty).toSet()
            var matched: Result? = null
            for (candidate in matchedResults) {
                if (undecidedFromReport.remove(candidate)) {
                    matched = candidate
                    break
                }
            }

            if (matched != null) {
                //leads to eliminating problems with the same hash
                state.put(matched, BaselineState.UNCHANGED, "equalIndicator")
            } else {
                if (!options.wasChecked.apply(result)) {
                    state.put(result, BaselineState.UNCHANGED)
                } else {
                    undecidedFromBaseline.add(result)
                }
            }
        }

    undecidedFromBaseline.removeAll { baselineResult ->
        for (indicator in baselineResult.extendedIndicators) {
            val candidates = extendedReportIndex.getOrEmpty(indicator).filter { it in undecidedFromReport }
            if (candidates.isEmpty()) continue
            val resolution = TiebreakerCascade.resolve(baselineResult, candidates) ?: continue
            val matchedBy = indicator.substringBefore(':') +
                (resolution.resolvedBy?.let { "+$it" } ?: "")
            undecidedFromReport.remove(resolution.result)
            state.put(resolution.result, BaselineState.UNCHANGED, matchedBy)
            return@removeAll true
        }
        false
    }

    // ResultKey fallback for baselines/reports without extended fingerprint data.
    val baselineByKey = HashMap<ResultKey, ArrayDeque<Result>>()
    undecidedFromBaseline.forEach { baselineByKey.getOrPut(ResultKey(it)) { ArrayDeque() }.addLast(it) }

    undecidedFromReport.forEach { result ->
        val matched = baselineByKey[ResultKey(result)]?.removeFirstOrNull() ?: return@forEach
        undecidedFromBaseline.remove(matched)
        undecidedFromReport.remove(result)
        state.put(result, BaselineState.UNCHANGED, "resultKey")
    }

    undecidedFromReport.forEach { result ->
        state.put(result, BaselineState.NEW)
    }

    undecidedFromBaseline.forEach { result ->
        val added = state.put(result, BaselineState.ABSENT)
        if (added && reportDescriptors.findById(result.ruleId) == null) {
            baselineDescriptors.findById(result.ruleId)?.addTo(report)
        }
    }

    report.withResults(state.results)

    return state
}
