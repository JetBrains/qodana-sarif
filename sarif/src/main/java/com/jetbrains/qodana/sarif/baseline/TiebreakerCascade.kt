package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.PhysicalLocation
import com.jetbrains.qodana.sarif.model.Result
import java.util.IdentityHashMap
import kotlin.math.abs

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

/**
 * The per-[Result] signals the cascade compares, parsed once. A result is typically scored against several
 * candidates, so extracting its snippet/location here keeps that work out of the inner pairwise loop.
 */
internal class ResultFeatures(val result: Result) {
    private val physicalLocation: PhysicalLocation? = result.locations?.firstOrNull()?.physicalLocation

    val contextSnippetText: String? = physicalLocation?.contextRegion?.snippet?.text
    val snippetText: String? = physicalLocation?.region?.snippet?.text
    val enclosingScopeName: String? =
        result.partialFingerprints?.getLastValue(BaselineCalculation.ENCLOSING_SCOPE_INDICATOR)
    val startLine: Int? = physicalLocation?.region?.startLine
    val startColumn: Int? = physicalLocation?.region?.startColumn
    private val contextStartLine: Int? = physicalLocation?.contextRegion?.startLine

    /** Context snippet split into trimmed lines; computed once and reused across every comparison. */
    val contextSnippetLines: List<String>? by lazy(LazyThreadSafetyMode.NONE) {
        contextSnippetText?.split('\n')?.map(String::trim)
    }

    /** Index of the flagged line within [contextSnippetLines], or null when it cannot be located. */
    val problemLineIndex: Int? by lazy(LazyThreadSafetyMode.NONE) {
        val lines = contextSnippetLines ?: return@lazy null
        val contextStart = contextStartLine ?: return@lazy null
        val problemStart = startLine ?: return@lazy null
        val index = problemStart - contextStart
        if (index !in lines.indices) return@lazy null
        val problemSnippet = snippetText?.trim()
        if (!problemSnippet.isNullOrEmpty() && '\n' !in problemSnippet && !lines[index].contains(problemSnippet)) {
            return@lazy null
        }
        index
    }

    private val contextSimilarityCache = IdentityHashMap<ResultFeatures, Double>()

    /**
     * Memoized context-snippet similarity of `(this, other)`. The same pair is scored once when ranking
     * candidates and again when tiebreaking, and the underlying LCS is too costly to repeat per pass.
     */
    fun contextSnippetSimilarityTo(other: ResultFeatures, compute: () -> Double): Double =
        contextSimilarityCache.getOrPut(other, compute)
}

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
        "enclosingScope" to ::filterByEnclosingScopeName,
        "columnDelta" to ::filterByColumnDelta,
        "lineDelta" to ::filterByLineDelta,
    )

    fun resolve(reportResultFeatures: ResultFeatures, candidates: List<ResultFeatures>): TiebreakerResolution? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return TiebreakerResolution(candidates[0].result, resolvedBy = null)

        var survivors = candidates
        for ((name, filter) in FILTERS) {
            survivors = filter(reportResultFeatures, survivors) ?: continue
            if (survivors.size == 1) return TiebreakerResolution(survivors[0].result, resolvedBy = name)
        }
        return TiebreakerResolution(survivors[0].result, resolvedBy = "fallback")
    }

    fun score(a: Result, b: Result): MatchScore = score(ResultFeatures(a), ResultFeatures(b))

    /**
     * Absolute quality of a single (report, baseline) pair. Component order mirrors [FILTERS] so the
     * lexicographic comparison reproduces the cascade priority without depending on a candidate set.
     */
    fun score(a: ResultFeatures, b: ResultFeatures): MatchScore = MatchScore(
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
     * keeps its own line far more reliably than its surroundings.
     */
    private fun contextSnippetSimilarity(a: ResultFeatures, b: ResultFeatures): Double =
        a.contextSnippetSimilarityTo(b) { computeContextSnippetSimilarity(a, b) }

    private fun computeContextSnippetSimilarity(a: ResultFeatures, b: ResultFeatures): Double {
        val aLines = a.contextSnippetLines ?: return 0.0
        val bLines = b.contextSnippetLines ?: return 0.0
        val snippetSim = SequenceSimilarity.similarity(aLines, bLines)

        val ai = a.problemLineIndex ?: return snippetSim
        val bi = b.problemLineIndex ?: return snippetSim
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
        return SequenceSimilarity.similarity(a, b)
    }

    private fun closeness(x: Int?, y: Int?): Double =
        if (x != null && y != null) -abs(x - y).toDouble() else Double.NEGATIVE_INFINITY

    private fun filterByContextSnippetSimilarity(reportResultFeatures: ResultFeatures, candidates: List<ResultFeatures>): List<ResultFeatures>? {
        if (reportResultFeatures.contextSnippetText == null) return null
        return filterByBestScore(candidates) { c -> if (c.contextSnippetText != null) contextSnippetSimilarity(reportResultFeatures, c) else null }
    }

    private fun filterBySnippet(reportResultFeatures: ResultFeatures, candidates: List<ResultFeatures>): List<ResultFeatures>? {
        val target = reportResultFeatures.snippetText ?: return null
        return candidates.filter { it.snippetText == target }.ifEmpty { null }
    }

    private fun filterByEnclosingScopeName(reportResultFeatures: ResultFeatures, candidates: List<ResultFeatures>): List<ResultFeatures>? {
        val target = reportResultFeatures.enclosingScopeName ?: return null
        return candidates.filter { it.enclosingScopeName == target }.ifEmpty { null }
    }

    private fun filterByLineDelta(reportResultFeatures: ResultFeatures, candidates: List<ResultFeatures>): List<ResultFeatures>? {
        val target = reportResultFeatures.startLine ?: return null
        return filterByBestScore(candidates, gap = 0.0) { c -> c.startLine?.let { -abs(it - target).toDouble() } }
    }

    private fun filterByColumnDelta(reportResultFeatures: ResultFeatures, candidates: List<ResultFeatures>): List<ResultFeatures>? {
        val target = reportResultFeatures.startColumn ?: return null
        return filterByBestScore(candidates, gap = 0.0) { c -> c.startColumn?.let { -abs(it - target).toDouble() } }
    }

    private inline fun filterByBestScore(
        candidates: List<ResultFeatures>,
        gap: Double = SIMILARITY_GAP,
        score: (ResultFeatures) -> Double?
    ): List<ResultFeatures>? {
        val scored = candidates.mapNotNull { c -> score(c)?.let { c to it } }
        if (scored.isEmpty()) return null
        val best = scored.maxOf { it.second }
        return scored.filter { best - it.second <= gap }.map { it.first }.ifEmpty { null }
    }
}
