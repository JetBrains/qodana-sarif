package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.PhysicalLocation
import com.jetbrains.qodana.sarif.model.Result
import java.util.IdentityHashMap
import kotlin.math.abs

private const val PRECISION = 1e-9
private const val SIMILARITY_THRESHOLD = 0.15

/**
 * Collision resolver signals, in priority order. Every entry but [FALLBACK] is one [MatchScore] component (by ordinal);
 * [FALLBACK] names a tie no signal breaks. Being last, it lets phases trust "up to signal X" via enum order.
 */
internal enum class Signal(val label: String, val tolerance: Double = PRECISION) {
    CONTEXT_SNIPPET_SIMILARITY("contextSnippetSimilarity", SIMILARITY_THRESHOLD),
    SNIPPET("snippet"),
    ENCLOSING_SCOPE("enclosingScope"),
    COLUMN_DELTA("columnDelta"),
    LINE_DELTA("lineDelta"),
    FALLBACK("fallback"),
}

/**
 * An absolute, order-independent quality of a single (report, baseline) pair, used both to rank candidates
 * for global assignment and to name the signal that decided a contest. Components are the per-signal values
 * in [Signal] order, compared lexicographically (higher is better).
 */
internal class MatchScore(@JvmField val components: DoubleArray) : Comparable<MatchScore> {

    /** Lexicographic order with [PRECISION] tolerance. Absorbs floating-point noise
     * to prevent misordering equal pairs, while preserving transitivity for distinct scores.
     */
    override fun compareTo(other: MatchScore): Int {
        for (i in components.indices) {
            val diff = components[i] - other.components[i]
            if (abs(diff) > PRECISION) return if (diff > 0) 1 else -1
        }
        return 0
    }

    /**
     * The highest-priority signal where this score strictly beats [rival] (exceeding its tolerance).
     * Returns `null` if no rival exists, or [Signal.FALLBACK] if no signal decisively breaks the tie.
     */
    fun decidingSignalAgainst(rival: MatchScore?): Signal? {
        if (rival == null) return null
        for (i in components.indices) {
            val signal = Signal.entries[i]
            if (components[i] - rival.components[i] > signal.tolerance) return signal
        }
        return Signal.FALLBACK
    }
}

/**
 * The per-[Result] signals the cascade compares, parsed once. A result is typically scored against several
 * candidates, so extracting its snippet/location here keeps that work out of the inner pairwise loop.
 */
internal class ResultFeatures(val result: Result) {
    private val physicalLocation: PhysicalLocation? = result.locations?.firstOrNull()?.physicalLocation
    private val contextStartLine: Int? = physicalLocation?.contextRegion?.startLine
    private val contextSimilarityCache = IdentityHashMap<ResultFeatures, Double>()

    val contextSnippetText: String? = physicalLocation?.contextRegion?.snippet?.text
    val snippetText: String? = physicalLocation?.region?.snippet?.text
    val enclosingScopeName: String? =
        result.partialFingerprints?.getLastValue(BaselineCalculation.ENCLOSING_SCOPE_INDICATOR)
    val startLine: Int? = physicalLocation?.region?.startLine
    val startColumn: Int? = physicalLocation?.region?.startColumn

    /** Context snippet split into trimmed lines; computed once and reused across every comparison. */
    val contextSnippetLines: List<String>? by lazy(LazyThreadSafetyMode.NONE) {
        contextSnippetText?.lineSequence()?.map(String::trim)?.toList()
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

    /** The flagged line as word/punctuation tokens (see [tokenizeLine]); computed once, reused across pairs. */
    val problemLineTokens: List<String>? by lazy(LazyThreadSafetyMode.NONE) {
        val index = problemLineIndex ?: return@lazy null
        tokenizeLine(contextSnippetLines!![index])
    }

    /**
     * Memoized context-snippet similarity of `(this, other)`. The same pair is scored once when ranking
     * candidates and again when tiebreaking, and the underlying LCS is too costly to repeat per pass.
     */
    fun contextSnippetSimilarityTo(other: ResultFeatures, compute: () -> Double): Double =
        contextSimilarityCache.getOrPut(other, compute)

    /** Builds at most one [ResultFeatures] per [Result] (by identity), so each result is parsed once. */
    class Cache : (Result) -> ResultFeatures {
        private val byResult = IdentityHashMap<Result, ResultFeatures>()
        override fun invoke(result: Result): ResultFeatures = byResult.getOrPut(result) { ResultFeatures(result) }
    }
}

internal object TiebreakerCascade {
    /**
     * Share of the context-snippet score carried by the single problem line; the rest is the
     * surrounding context, which is prone to incidental overlap.
     */
    private const val PROBLEM_LINE_WEIGHT = 0.8

    fun score(a: Result, b: Result): MatchScore = score(ResultFeatures(a), ResultFeatures(b))

    /**
     * Absolute quality of a single (report, baseline) pair. Component order mirrors [Signal] so the
     * lexicographic comparison reproduces the tiebreaker priority without depending on a candidate set.
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

    /** Similarity of two context snippets compared line-by-line (LCS over trimmed lines) */
    private fun contextSnippetSimilarity(a: ResultFeatures, b: ResultFeatures): Double =
        a.contextSnippetSimilarityTo(b) { computeContextSnippetSimilarity(a, b) }

    private fun computeContextSnippetSimilarity(a: ResultFeatures, b: ResultFeatures): Double {
        val aLines = a.contextSnippetLines ?: return 0.0
        val bLines = b.contextSnippetLines ?: return 0.0
        val snippetSim = SequenceSimilarity.similarity(aLines, bLines)

        // The problem line is compared token-by-token (words + structural punctuation)
        val aTokens = a.problemLineTokens ?: return snippetSim
        val bTokens = b.problemLineTokens ?: return snippetSim
        val problemLineSim = SequenceSimilarity.similarity(aTokens, bTokens)
        return PROBLEM_LINE_WEIGHT * problemLineSim + (1 - PROBLEM_LINE_WEIGHT) * snippetSim
    }

    /** Negated distance so nearer is greater; [Double.NEGATIVE_INFINITY] when either side lacks the coordinate. */
    private fun closeness(x: Int?, y: Int?): Double =
        if (x != null && y != null) -abs(x - y).toDouble() else Double.NEGATIVE_INFINITY
}
