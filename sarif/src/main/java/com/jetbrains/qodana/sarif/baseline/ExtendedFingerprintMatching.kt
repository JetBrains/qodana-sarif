package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Result.BaselineState

internal object ExtendedFingerprintMatching {
    private val TIER_VERSIONS = intArrayOf(4, 3, 2, 1)

    fun match(
        undecidedReport: IdentitySet<Result>,
        undecidedBaseline: MutableList<Result>,
        state: DiffState
    ) {
        // Build report fingerprint indices for all tiers
        val reportIndex = HashMap<Int, MultiMap<String, Result>>()
        for (version in TIER_VERSIONS) reportIndex[version] = MultiMap()
        for (result in undecidedReport) {
            for (version in TIER_VERSIONS) {
                extendedFingerprint(result, version)?.let { reportIndex.getValue(version).add(it, result) }
            }
        }

        // For each baseline result, try tiers from strongest to weakest
        val iter = undecidedBaseline.iterator()
        while (iter.hasNext()) {
            val baselineResult = iter.next()
            for (version in TIER_VERSIONS) {
                val hash = extendedFingerprint(baselineResult, version) ?: continue
                val candidates = reportIndex.getValue(version).getOrEmpty(hash).filter { it in undecidedReport }
                if (candidates.isEmpty()) continue

                val resolution = TiebreakerCascade.resolve(baselineResult, candidates) ?: continue
                val matched = resolution.result
                val matchedBy = buildString {
                    append("extendedFingerprint/v$version")
                    resolution.resolvedBy?.let { append("+$it") }
                }
                undecidedReport.remove(matched)
                iter.remove()
                state.put(matched, BaselineState.UNCHANGED, matchedBy)
                break
            }
        }
    }

    private fun extendedFingerprint(result: Result, version: Int): String? =
        result.extendedFingerprints?.get(BaselineCalculation.EXTENDED_FINGERPRINT, version)
}
