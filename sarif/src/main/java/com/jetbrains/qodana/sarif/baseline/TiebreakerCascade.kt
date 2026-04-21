package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result
import kotlin.math.abs

internal data class TiebreakerResolution(val result: Result, val resolvedBy: String?)

private val Result.contextSnippetText: String?
    get() = locations?.firstOrNull()?.physicalLocation?.contextRegion?.snippet?.text

private val Result.snippetText: String?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.snippet?.text

private val Result.astShape: String?
    get() = properties?.get("astShape") as? String

private val Result.filePath: String?
    get() = locations?.firstOrNull()?.physicalLocation?.artifactLocation?.uri

private val Result.startLine: Int?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.startLine

internal object TiebreakerCascade {
    private const val SIMILARITY_GAP = 0.15

    private val FILTERS = listOf(
        "contextSnippet" to ::filterByContextSnippet,
        "snippet" to ::filterBySnippet,
        "shapeContainment" to ::filterByShapeContainment,
        "shapeSimilarity" to ::filterByShapeSimilarity,
        "pathSimilarity" to ::filterByPathSimilarity,
        "lineDelta" to ::filterByLineDelta
    )

    fun resolve(baselineResult: Result, candidates: List<Result>): TiebreakerResolution? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return TiebreakerResolution(candidates[0], resolvedBy = null)

        var survivors = candidates
        for ((name, filter) in FILTERS) {
            survivors = filter(baselineResult, survivors) ?: continue
            if (survivors.size == 1) return TiebreakerResolution(survivors[0], resolvedBy = name)
        }
        return TiebreakerResolution(survivors[0], resolvedBy = null)
    }

    private fun filterByContextSnippet(baselineResult: Result, candidates: List<Result>): List<Result>? {
        val baselineContext = baselineResult.contextSnippetText ?: return null
        return candidates.filter { it.contextSnippetText == baselineContext }.ifEmpty { null }
    }

    private fun filterBySnippet(baselineResult: Result, candidates: List<Result>): List<Result>? {
        val baselineSnippet = baselineResult.snippetText ?: return null
        return candidates.filter { it.snippetText == baselineSnippet }.ifEmpty { null }
    }

    private fun filterByShapeContainment(baselineResult: Result, candidates: List<Result>): List<Result>? {
        val baselineShape = baselineResult.astShape ?: return null
        return candidates.filter { candidate ->
            val candidateShape = candidate.astShape ?: return@filter false
            LcsSimilarity.isSubsequence(baselineShape, candidateShape) ||
                LcsSimilarity.isSubsequence(candidateShape, baselineShape)
        }.ifEmpty { null }
    }

    private fun filterByShapeSimilarity(baselineResult: Result, candidates: List<Result>): List<Result>? {
        val baselineShape = baselineResult.astShape ?: return null
        return filterByBestScore(candidates) { it.astShape?.let { s -> LcsSimilarity.similarity(baselineShape, s) } }
    }

    private fun filterByPathSimilarity(baselineResult: Result, candidates: List<Result>): List<Result>? {
        val baselinePath = baselineResult.filePath ?: return null
        return filterByBestScore(candidates) { it.filePath?.let { p -> LcsSimilarity.similarity(baselinePath, p) } }
    }

    private fun filterByLineDelta(baselineResult: Result, candidates: List<Result>): List<Result>? {
        val baselineLine = baselineResult.startLine ?: return null
        return filterByBestScore(candidates, gap = 0.0) { c ->
            c.startLine?.let { -abs(it - baselineLine).toDouble() }
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
