package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result

internal interface BaselineMatcher {
    fun findMatch(baseline: Result, candidates: Set<Result>): MatchResult?
}

internal data class MatchResult(val result: Result, val matchedBy: String, val matchedWith: String? = null)

/** Matches via content equality (ruleId + message + URI + snippet + charLength + level) using [ResultKey]. */
internal class ResultKeyMatcher(reportResults: Iterable<Result>) : BaselineMatcher {
    private val index: Map<ResultKey, List<Result>> =
        reportResults.groupByTo(HashMap()) { ResultKey(it) }

    override fun findMatch(baseline: Result, candidates: Set<Result>): MatchResult? {
        val available = (index[ResultKey(baseline)] ?: return null).filter { it in candidates }
        if (available.isEmpty()) return null
        val resolution = TiebreakerCascade.resolve(baseline, available) ?: return null
        return MatchResult(resolution.result, "resultKey" + (resolution.resolvedBy?.let { "+$it" } ?: ""))
    }
}

/** Matches via a single partialFingerprint key. [acceptTiebreaker] lets the caller reject specific tiebreaker outcomes */
internal class HashMatcher(
    reportResults: Iterable<Result>,
    private val key: String,
    private val acceptTiebreaker: (resolvedBy: String?) -> Boolean = { true },
) : BaselineMatcher {
    private val index: List<Result> = reportResults.filter { it.partialFingerprints?.getValues(key) != null }

    override fun findMatch(baseline: Result, candidates: Set<Result>): MatchResult? {
        val pairs = index
            .filter { it in candidates }
            .mapNotNull { c -> greatestCommonVersionMatch(baseline, c, key)?.let { v -> c to v } }
        if (pairs.isEmpty()) return null
        val resolution = TiebreakerCascade.resolve(baseline, pairs.map { it.first }) ?: return null
        if (!acceptTiebreaker(resolution.resolvedBy)) return null
        val version = pairs.first { it.first === resolution.result }.second
        val baselineId = baseline.partialFingerprints?.getLastValue("equalIndicator")
        return MatchResult(resolution.result, "$key/v$version" + (resolution.resolvedBy?.let { "+$it" } ?: ""),  baselineId ?: "")
    }
}

/** Two results match on a fingerprint key only at the LATEST version present in both partialFingerprints. */
private fun greatestCommonVersionMatch(a: Result, b: Result, key: String): Int? {
    val av = a.partialFingerprints?.getValues(key) ?: return null
    val bv = b.partialFingerprints?.getValues(key) ?: return null
    val maxCommon = av.keys.filter { it in bv.keys }.maxOrNull() ?: return null
    return if (av[maxCommon] == bv[maxCommon]) maxCommon else null
}
