package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result

internal interface BaselineMatcher {
    fun findMatch(baseline: Result, candidates: Set<Result>): MatchResult?
}

internal data class MatchResult(val result: Result, val matchedBy: String)

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

/**
 * Matches via a priority-ordered cascade of partialFingerprint keys. Handles both single-key phases
 * (e.g. `equalIndicator`) and the multi-key structural cascade (postFuncRef → funcStruct → coreProblem).
 *
 * [acceptTiebreaker] lets the caller reject specific (key, tiebreaker) combinations. When it returns
 * false, the cascade falls through to the next key (or to no match if this was the last one). Default
 * accepts everything.
 */
internal class HashCascadeMatcher(
    reportResults: Iterable<Result>,
    private val keys: List<String>,
    private val acceptTiebreaker: (key: String, resolvedBy: String?) -> Boolean = { _, _ -> true },
) : BaselineMatcher {
    private val index: Map<String, List<Result>> = keys.associateWith { key ->
        reportResults.filter { it.partialFingerprints?.getValues(key) != null }
    }

    override fun findMatch(baseline: Result, candidates: Set<Result>): MatchResult? {
        for (key in keys) {
            val pairs = index.getValue(key)
                .filter { it in candidates }
                .mapNotNull { c -> greatestCommonVersionMatch(baseline, c, key)?.let { v -> c to v } }
            if (pairs.isEmpty()) continue
            val resolution = TiebreakerCascade.resolve(baseline, pairs.map { it.first }) ?: continue
            if (!acceptTiebreaker(key, resolution.resolvedBy)) continue
            val version = pairs.first { it.first === resolution.result }.second
            return MatchResult(resolution.result, "$key/v$version" + (resolution.resolvedBy?.let { "+$it" } ?: ""))
        }
        return null
    }
}

/** Two results match on a fingerprint key only at the LATEST version present in both partialFingerprints. */
private fun greatestCommonVersionMatch(a: Result, b: Result, key: String): Int? {
    val av = a.partialFingerprints?.getValues(key) ?: return null
    val bv = b.partialFingerprints?.getValues(key) ?: return null
    val maxCommon = av.keys.filter { it in bv.keys }.maxOrNull() ?: return null
    return if (av[maxCommon] == bv[maxCommon]) maxCommon else null
}
