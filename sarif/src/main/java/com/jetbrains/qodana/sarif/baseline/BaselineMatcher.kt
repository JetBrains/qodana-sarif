package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result

/**
 * Produces every admissible (reportResult, baselineResult) candidate pairing for one matching phase — is what lets the
 * strongest pair win first, so a weaker pair can never steal a baseline (or report) from a stronger one.
 */
internal interface BaselineMatcher {
    fun candidates(undecidedFromReport: List<Result>, undecidedFromBaseline: Set<Result>): List<MatchCandidate>
}

internal data class MatchCandidate(
    val reportResult: Result,
    val baselineResult: Result,
    val matchedBy: String,
    val matchedWith: String,
)

private fun Result.equalIndicator(): String =
    partialFingerprints?.getLastValue("equalIndicator") ?: ""

private fun candidate(reportResult: Result, baselineResult: Result, matchedBy: String): MatchCandidate =
    MatchCandidate(reportResult, baselineResult, matchedBy, baselineResult.equalIndicator())

/** Matches via content equality using [ResultKey]. */
internal class ResultKeyMatcher(baselineResults: Iterable<Result>) : BaselineMatcher {
    private val index: Map<ResultKey, List<Result>> =
        baselineResults.groupByTo(HashMap()) { ResultKey(it) }

    override fun candidates(undecidedFromReport: List<Result>, undecidedFromBaseline: Set<Result>): List<MatchCandidate> =
        undecidedFromReport.flatMap { reportResult ->
            index[ResultKey(reportResult)].orEmpty()
                .filter { it in undecidedFromBaseline }
                .map { baselineResult -> candidate(reportResult, baselineResult, "resultKey") }
        }
}

/** Matches via a single partialFingerprint key. */
internal class HashMatcher(
    baselineResults: Iterable<Result>,
    private val fingerprintKey: String,
) : BaselineMatcher {
    private val index: List<Result> =
        baselineResults.filter { it.partialFingerprints?.getValues(fingerprintKey) != null }

    override fun candidates(undecidedFromReport: List<Result>, undecidedFromBaseline: Set<Result>): List<MatchCandidate> {
        val candidates = ArrayList<MatchCandidate>()
        for (reportResult in undecidedFromReport) {
            val candidate = index
                .filter { it in undecidedFromBaseline }
                .mapNotNull { baselineResult -> greatestCommonVersionMatch(reportResult, baselineResult, fingerprintKey)?.let { baselineResult to it } }
            if (candidate.isEmpty()) continue
            candidate.mapTo(candidates) { (baselineResult, version) -> candidate(reportResult, baselineResult, "$fingerprintKey/v$version") }
        }
        return candidates
    }
}

/** Two results match on a fingerprint key only at the LATEST version present in both partialFingerprints. */
private fun greatestCommonVersionMatch(a: Result, b: Result, fingerprintKey: String): Int? {
    val av = a.partialFingerprints?.getValues(fingerprintKey) ?: return null
    val bv = b.partialFingerprints?.getValues(fingerprintKey) ?: return null
    val maxCommon = av.keys.filter { it in bv.keys }.maxOrNull() ?: return null
    return if (av[maxCommon] == bv[maxCommon]) maxCommon else null
}
