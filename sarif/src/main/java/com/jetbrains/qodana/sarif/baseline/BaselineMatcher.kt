package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result

/**
 * Produces every admissible (report, baseline) candidate pairing for one matching phase — is what lets the
 * strongest pair win first, so a weaker pair can never steal a baseline (or report) from a stronger one.
 */
internal interface BaselineMatcher {
    fun candidates(reports: List<Result>, baselines: Set<Result>): List<MatchCandidate>
}

internal data class MatchCandidate(
    val report: Result,
    val baseline: Result,
    val baseLabel: String,
    val matchedWith: String,
)

private fun Result.equalIndicator(): String =
    partialFingerprints?.getLastValue("equalIndicator") ?: ""

private fun candidate(report: Result, baseline: Result, label: String): MatchCandidate =
    MatchCandidate(report, baseline, label, baseline.equalIndicator())

/** Matches via content equality using [ResultKey]. */
internal class ResultKeyMatcher(baselineResults: Iterable<Result>) : BaselineMatcher {
    private val index: Map<ResultKey, List<Result>> =
        baselineResults.groupByTo(HashMap()) { ResultKey(it) }

    override fun candidates(reports: List<Result>, baselines: Set<Result>): List<MatchCandidate> =
        reports.flatMap { report ->
            index[ResultKey(report)].orEmpty()
                .filter { it in baselines }
                .map { baseline -> candidate(report, baseline, "resultKey") }
        }
}

/** Matches via a single partialFingerprint key. */
internal class HashMatcher(
    baselineResults: Iterable<Result>,
    private val key: String,
    private val eligible: (Result) -> Boolean = { true }
) : BaselineMatcher {
    private val index: List<Result> =
        baselineResults.filter { it.partialFingerprints?.getValues(key) != null && eligible(it) }

    override fun candidates(reports: List<Result>, baselines: Set<Result>): List<MatchCandidate> {
        val out = ArrayList<MatchCandidate>()
        for (report in reports) {
            if (!eligible(report)) continue
            val viable = index
                .filter { it in baselines }
                .mapNotNull { baseline -> greatestCommonVersionMatch(report, baseline, key)?.let { baseline to it } }
            if (viable.isEmpty()) continue
            viable.mapTo(out) { (baseline, version) -> candidate(report, baseline, "$key/v$version") }
        }
        return out
    }
}

/** Two results match on a fingerprint key only at the LATEST version present in both partialFingerprints. */
private fun greatestCommonVersionMatch(a: Result, b: Result, key: String): Int? {
    val av = a.partialFingerprints?.getValues(key) ?: return null
    val bv = b.partialFingerprints?.getValues(key) ?: return null
    val maxCommon = av.keys.filter { it in bv.keys }.maxOrNull() ?: return null
    return if (av[maxCommon] == bv[maxCommon]) maxCommon else null
}
