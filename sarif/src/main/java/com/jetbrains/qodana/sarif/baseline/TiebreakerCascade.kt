package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result
import kotlin.math.abs

internal data class TiebreakerResolution(val result: Result, val resolvedBy: String?)

private val Result.contextSnippetText: String?
    get() = locations?.firstOrNull()?.physicalLocation?.contextRegion?.snippet?.text

private val Result.snippetText: String?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.snippet?.text

private val Result.astPath: String?
    get() = properties?.get("normalizedAstPath") as? String

private val Result.startLine: Int?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.startLine

private val Result.startColumn: Int?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.startColumn

internal object TiebreakerCascade {
    private const val SIMILARITY_GAP = 0.15

    private val FILTERS = listOf(
        "contextSnippet" to ::filterByContextSnippet,
        "snippet" to ::filterBySnippet,
        "astPathSimilarity" to ::filterByAstPathSimilarity,
        "lineDelta" to ::filterByLineDelta,
        "columnDelta" to ::filterByColumnDelta,
    )

    fun resolve(baselineResult: Result, candidates: List<Result>): TiebreakerResolution? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return TiebreakerResolution(candidates[0], resolvedBy = null)

        var survivors = candidates
        for ((name, filter) in FILTERS) {
            survivors = filter(baselineResult, survivors) ?: continue
            if (survivors.size == 1) return TiebreakerResolution(survivors[0], resolvedBy = name)
        }
        return TiebreakerResolution(survivors[0], resolvedBy = "fallback")
    }

    private fun filterByContextSnippet(baseline: Result, candidates: List<Result>): List<Result>? {
        val target = baseline.contextSnippetText ?: return null
        return candidates.filter { it.contextSnippetText == target }.ifEmpty { null }
    }

    private fun filterBySnippet(baseline: Result, candidates: List<Result>): List<Result>? {
        val target = baseline.snippetText ?: return null
        return candidates.filter { it.snippetText == target }.ifEmpty { null }
    }

    private fun filterByAstPathSimilarity(baseline: Result, candidates: List<Result>): List<Result>? {
        val target = baseline.astPath ?: return null
        return filterByBestScore(candidates) { it.astPath?.let { p -> PathSimilarity.similarity(target, p) } }
    }

    private fun filterByLineDelta(baseline: Result, candidates: List<Result>): List<Result>? {
        val target = baseline.startLine ?: return null
        return filterByBestScore(candidates, gap = 0.0) { c ->
            c.startLine?.let { -abs(it - target).toDouble() }
        }
    }

    private fun filterByColumnDelta(baseline: Result, candidates: List<Result>): List<Result>? {
        val target = baseline.startColumn ?: return null
        return filterByBestScore(candidates, gap = 0.0) { c ->
            c.startColumn?.let { -abs(it - target).toDouble() }
        }
    }

    private inline fun filterByBestScore(
        candidates: List<Result>,
        gap: Double = SIMILARITY_GAP,
        score: (Result) -> Double?
    ): List<Result>? {
        val scored = candidates.mapNotNull { c -> score(c)?.let { c to it } }
        if (scored.isEmpty()) return null
        val best = scored.maxOf { it.second }
        return scored.filter { best - it.second <= gap }.map { it.first }.ifEmpty { null }
    }
}
