package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result

/**
 * Produces every admissible (reportResult, baselineResult) candidate pairing for one matching phase.
 * Each matcher is built over only the remaining baseline results for its phase.
 */
internal interface BaselineMatcher {
    fun candidates(undecidedFromReport: Collection<Result>): List<MatchCandidate>
}

internal data class MatchCandidate(
    val reportResult: Result,
    val baselineResult: Result,
    val matchedBy: String,
)

/** Matches via content equality using [ResultKey]. Kept for reuse (e.g. a ResultKey fallback phase later). */
@Suppress("unused")
internal class ResultKeyMatcher(baselineResults: Iterable<Result>) : BaselineMatcher {
    private val index: Map<ResultKey, List<Result>> =
        baselineResults.groupByTo(HashMap()) { ResultKey(it) }

    override fun candidates(undecidedFromReport: Collection<Result>): List<MatchCandidate> =
        undecidedFromReport.flatMap { reportResult ->
            index[ResultKey(reportResult)].orEmpty()
                .map { baselineResult -> MatchCandidate(reportResult, baselineResult, "resultKey") }
        }
}

/** Matches via a single partialFingerprint key. Two results match at the greatest fingerprint version they share */
internal class HashMatcher(
    baselineResults: Iterable<Result>,
    private val fingerprintKey: String,
) : BaselineMatcher {
    // version -> (value -> baselines). Nested to avoid allocating a Pair key per fingerprint entry and lookup.
    private val byVersionValue = HashMap<Int, HashMap<String, MutableList<Result>>>()

    init {
        for (baselineResult in baselineResults) {
            val fingerprints = baselineResult.partialFingerprints?.getValues(fingerprintKey) ?: continue
            for ((fpVersion, fpValue) in fingerprints) {
                byVersionValue.getOrPut(fpVersion) { HashMap() }.getOrPut(fpValue) { ArrayList() }.add(baselineResult)
            }
        }
    }

    override fun candidates(undecidedFromReport: Collection<Result>): List<MatchCandidate> {
        if (byVersionValue.isEmpty()) return emptyList()
        val candidates = ArrayList<MatchCandidate>()
        for (reportResult in undecidedFromReport) {
            val fingerprints = reportResult.partialFingerprints?.getValues(fingerprintKey) ?: continue
            val reportVersions = fingerprints.keys
            for ((fpVersion, fpValue) in fingerprints) {
                val matchingBaselines = byVersionValue[fpVersion]?.get(fpValue) ?: continue
                for (baselineResult in matchingBaselines) {
                    val baselineVersions = baselineResult.partialFingerprints?.getValues(fingerprintKey)?.keys ?: continue
                     // Restricts candidate generation to the greatest shared hash version
                     // If two items share multiple versions, only the latest mutual match is emitted—all older versions are ignored
                    if (fpVersion == maxSharedVersion(reportVersions, baselineVersions)) {
                        candidates.add(MatchCandidate(reportResult, baselineResult, "$fingerprintKey/v$fpVersion"))
                    }
                }
            }
        }
        return candidates
    }

    /** Greatest version contained in both sets, or null when they are disjoint. */
    private fun maxSharedVersion(a: Set<Int>, b: Set<Int>): Int? {
        val (small, large) = if (a.size <= b.size) a to b else b to a
        var max: Int? = null
        for (v in small) {
            if (v in large && (max == null || v > max)) max = v
        }
        return max
    }
}
