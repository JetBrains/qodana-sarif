package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.ENCLOSING_STATEMENT_INDICATOR
import com.jetbrains.qodana.sarif.model.Result
import kotlin.math.abs
import kotlin.text.substringAfterLast

internal data class TiebreakerResolution(val result: Result, val resolvedBy: String?)

/**
 * An absolute, order-independent quality of a single (report, baseline) pair, used to rank candidates
 * for global assignment. Components are the per-signal values in [TiebreakerCascade.FILTERS] order,
 * compared lexicographically (higher is better).
 */
internal class MatchScore(@JvmField val components: DoubleArray) : Comparable<MatchScore> {
    override fun compareTo(other: MatchScore): Int {
        for (i in components.indices) {
            val cmp = components[i].compareTo(other.components[i])
            if (cmp != 0) return cmp
        }
        return 0
    }
}

private val Result.contextSnippetText: String?
    get() = locations?.firstOrNull()?.physicalLocation?.contextRegion?.snippet?.text

private val Result.contextRegionStartLine: Int?
    get() = locations?.firstOrNull()?.physicalLocation?.contextRegion?.startLine

private fun Result.contextSnippetLines(): List<String>? =
    contextSnippetText?.split('\n')?.map(String::trim)

private fun Result.problemLineIndex(lines: List<String>): Int? {
    val contextStart = contextRegionStartLine ?: return null
    val problemStart = startLine ?: return null
    val problemSnippet = snippetText?.trim()
    val index = problemStart - contextStart
    if (index !in lines.indices) return null
    if (!problemSnippet.isNullOrEmpty() && '\n' !in problemSnippet && !lines[index].contains(problemSnippet)) return null
    return index
}

private val Result.snippetText: String?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.snippet?.text

private val Result.enclosingScopeName: String?
    get() = properties?.get("funcName") as? String
    //get() = partialFingerprints?.getLastValue(ENCLOSING_SCOPE_INDICATOR)?.substringAfterLast('#')

private val Result.startLine: Int?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.startLine

private val Result.startColumn: Int?
    get() = locations?.firstOrNull()?.physicalLocation?.region?.startColumn

internal object TiebreakerCascade {
    private const val SIMILARITY_GAP = 0.15

    /**
     * Share of the context-snippet score carried by the single flagged line; the rest is the
     * surrounding context. Weighted high because the flagged line is the reliable signal — the
     * surrounding lines are prone to incidental overlap.
     */
    private const val PROBLEM_LINE_WEIGHT = 0.8

    private val FILTERS = listOf(
        "contextSnippetSimilarity" to ::filterByContextSnippetSimilarity,
        "snippet" to ::filterBySnippet,
        "enclosingScopeName" to ::filterByEnclosingScopeName,
        "columnDelta" to ::filterByColumnDelta,
        "lineDelta" to ::filterByLineDelta,
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

    /**
     * Absolute quality of a single (report, baseline) pair. Component order mirrors [FILTERS] so the
     * lexicographic comparison reproduces the cascade priority without depending on a candidate set.
     */
    fun score(a: Result, b: Result): MatchScore = MatchScore(
        doubleArrayOf(
            contextSnippetSimilarity(a, b),
            eqScore(a.snippetText, b.snippetText),
            eqScore(a.enclosingScopeName, b.enclosingScopeName),
            closeness(a.startColumn, b.startColumn),
            closeness(a.startLine, b.startLine),
        )
    )

    private fun eqScore(x: String?, y: String?): Double = if (x != null && x == y) 1.0 else 0.0

    /**
     * Similarity of two context snippets compared line-by-line (LCS over trimmed lines).
     * The flagged line itself carries [PROBLEM_LINE_WEIGHT] of the score, because a problem that moved
     * keeps its own line far more reliably than its surroundings. The flagged line is located by [problemLineIndex];
     */
    private fun contextSnippetSimilarity(a: Result, b: Result): Double {
        val aLines = a.contextSnippetLines() ?: return 0.0
        val bLines = b.contextSnippetLines() ?: return 0.0
        val snippetSim = SequenceSimilarity.similarity(aLines, bLines)

        val ai = a.problemLineIndex(aLines) ?: return snippetSim
        val bi = b.problemLineIndex(bLines) ?: return snippetSim
        val problemLineSim = lineSimilarity(aLines[ai], bLines[bi])
        return PROBLEM_LINE_WEIGHT * problemLineSim + (1 - PROBLEM_LINE_WEIGHT) * snippetSim
    }

    /**
     * Character-level similarity of a single line (LCS over characters), so two renderings of the same
     * statement score high even when they differ only by surrounding keywords/punctuation.
     */
    private fun lineSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return SequenceSimilarity.similarity(a.map(Char::toString), b.map(Char::toString))
    }

    private fun closeness(x: Int?, y: Int?): Double =
        if (x != null && y != null) -abs(x - y).toDouble() else Double.NEGATIVE_INFINITY

    private fun filterByContextSnippetSimilarity(baseline: Result, candidates: List<Result>): List<Result>? {
        if (baseline.contextSnippetText == null) return null
        return filterByBestScore(candidates) { c -> if (c.contextSnippetText != null) contextSnippetSimilarity(baseline, c) else null }
    }

    private fun filterBySnippet(baseline: Result, candidates: List<Result>): List<Result>? {
        val target = baseline.snippetText ?: return null
        return candidates.filter { it.snippetText == target }.ifEmpty { null }
    }

    private fun filterByEnclosingScopeName(baseline: Result, candidates: List<Result>): List<Result>? {
        val target = baseline.enclosingScopeName ?: return null
        return candidates.filter { it.enclosingScopeName == target }.ifEmpty { null }
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
