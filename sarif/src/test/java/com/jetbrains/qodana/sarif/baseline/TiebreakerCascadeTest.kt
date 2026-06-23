package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [TiebreakerCascade] in isolation.
 *
 * Filter order:
 *   contextSnippetSimilarity → snippet → funcName → columnDelta → lineDelta.
 * If all filters tie or yield nothing, `resolvedBy` is "fallback".
 * If there is a single candidate from the start, `resolvedBy` is null.
 */
class TiebreakerCascadeTest {

    private fun result(
        snippet: String? = null,
        contextSnippet: String? = null,
        contextStartLine: Int? = null,
        funcName: String? = null,
        startLine: Int? = null,
        startColumn: Int? = null,
    ): Result {
        val r = Result(Message().withText("Empty slice declaration using a literal"))
        if (funcName != null) r.updateProperties { it["funcName"] = funcName }

        val region = Region()
        if (snippet != null) region.withSnippet(ArtifactContent().withText(snippet))
        if (startLine != null) region.withStartLine(startLine)
        if (startColumn != null) region.withStartColumn(startColumn)
        val physical = PhysicalLocation().withRegion(region)
        if (contextSnippet != null) {
            val contextRegion = Region().withSnippet(ArtifactContent().withText(contextSnippet))
            if (contextStartLine != null) contextRegion.withStartLine(contextStartLine)
            physical.withContextRegion(contextRegion)
        }
        r.withLocations(listOf(Location().withPhysicalLocation(physical)))
        return r
    }

    private fun resolve(baseline: Result, vararg candidates: Result) =
        TiebreakerCascade.resolve(baseline, candidates.toList())

    @Test
    fun `empty candidate list returns null`() {
        assertNull(resolve(result()))
    }

    @Test
    fun `single candidate is returned without any tiebreaker label`() {
        val only = result()
        val res = resolve(result(), only)!!
        assertSame(only, res.result)
        assertNull(res.resolvedBy)
    }

    @Test
    fun `contextSnippetSimilarity runs first and picks the candidate with matching context`() {
        val ctx = "before\nproblem\nafter"
        val res = resolve(
            result(contextSnippet = ctx),
            result(contextSnippet = ctx),
            result(contextSnippet = "different\n"),
        )!!
        assertEquals("contextSnippetSimilarity", res.resolvedBy)
    }

    @Test
    fun `snippet breaks ties when contextSnippet is absent`() {
        val res = resolve(
            result(snippet = "[]IP"),
            result(snippet = "[]IP"),
            result(snippet = "[]*net.IPNet"),
        )!!
        assertEquals("snippet", res.resolvedBy)
    }

    @Test
    fun `lineDelta picks the candidate with the smallest line distance`() {
        val res = resolve(
            result(startLine = 1224),
            result(startLine = 1226),
            result(startLine = 1500),
        )!!
        assertEquals("lineDelta", res.resolvedBy)
        assertEquals(1226, res.result.locations.first().physicalLocation.region.startLine)
    }

    @Test
    fun `columnDelta picks the candidate with the smallest column distance`() {
        val res = resolve(
            result(startLine = 37, startColumn = 31),
            result(startLine = 37, startColumn = 31),
            result(startLine = 37, startColumn = 58),
            result(startLine = 37, startColumn = 71),
        )!!
        assertEquals("columnDelta", res.resolvedBy)
        assertEquals(31, res.result.locations.first().physicalLocation.region.startColumn)
    }

    @Test
    fun `columnDelta runs before lineDelta when both could discriminate`() {
        // Candidate with the closest column wins even though another candidate is on a closer line.
        val res = resolve(
            result(startLine = 100, startColumn = 10),
            result(startLine = 130, startColumn = 10),  // far line, exact column
            result(startLine = 101, startColumn = 80),  // close line, far column
        )!!
        assertEquals("columnDelta", res.resolvedBy)
        assertEquals(130, res.result.locations.first().physicalLocation.region.startLine)
    }

    @Test
    fun `contextSnippetSimilarity prefers the most line-similar context`() {
        // The closest candidate shares 4 of 5 lines (a neighbour line was edited); the other shares 1.
        val res = resolve(
            result(contextSnippet = "a\nb\nPROBLEM\nc\nd"),
            result(contextSnippet = "a\nb\nPROBLEM\nc\nEDITED"),
            result(contextSnippet = "x\ny\nPROBLEM\nz\nw"),
        )!!
        assertEquals("contextSnippetSimilarity", res.resolvedBy)
        assertEquals("a\nb\nPROBLEM\nc\nEDITED", res.result.locations.first().physicalLocation.contextRegion.snippet.text)
    }

    @Test
    fun `contextSnippetSimilarity weights the flagged line above its surroundings`() {
        // Context starts at line 10, problem at line 12 → flagged line is index 2 ("PROBLEM").
        // Weighting the flagged line must let matchesLine win, the reverse of plain line-overlap.
        val baseline = result(contextSnippet = "a\nb\nPROBLEM\nc\nd", contextStartLine = 10, startLine = 12)
        val matchesLine = result(contextSnippet = "w\nx\nPROBLEM\ny\nz", contextStartLine = 10, startLine = 12)
        val matchesAround = result(contextSnippet = "a\nb\nDIFFERENT\nc\nd", contextStartLine = 10, startLine = 12)

        val res = resolve(baseline, matchesLine, matchesAround)!!
        assertEquals("contextSnippetSimilarity", res.resolvedBy)
        assertSame(matchesLine, res.result)
    }

    @Test
    fun `contextSnippetSimilarity handles a problem on the first line of the file`() {
        // Problem on line 1 with context also starting at line 1 → flagged line is index 0, not the middle.
        val baseline = result(contextSnippet = "PROBLEM\nb\nc", contextStartLine = 1, startLine = 1)
        val matchesLine = result(contextSnippet = "PROBLEM\ny\nz", contextStartLine = 1, startLine = 1)
        val matchesAround = result(contextSnippet = "OTHER\nb\nc", contextStartLine = 1, startLine = 1)

        val res = resolve(baseline, matchesLine, matchesAround)!!
        assertEquals("contextSnippetSimilarity", res.resolvedBy)
        assertSame(matchesLine, res.result)
    }

    @Test
    fun `contextSnippetSimilarity falls back to whole-snippet overlap without line numbers`() {
        // No region/contextRegion start lines → the flagged line can't be located, so the candidate
        // sharing more lines overall wins (4 of 5), unlike the weighted case above.
        val res = resolve(
            result(contextSnippet = "a\nb\nPROBLEM\nc\nd"),
            result(contextSnippet = "a\nb\nDIFFERENT\nc\nd"),
            result(contextSnippet = "w\nx\nPROBLEM\ny\nz"),
        )!!
        assertEquals("contextSnippetSimilarity", res.resolvedBy)
        assertEquals("a\nb\nDIFFERENT\nc\nd", res.result.locations.first().physicalLocation.contextRegion.snippet.text)
    }

    @Test
    fun `funcName breaks ties when content filters do not apply`() {
        val res = resolve(
            result(funcName = "computeTotals"),
            result(funcName = "computeTotals"),
            result(funcName = "renderHeader"),
        )!!
        assertEquals("funcName", res.resolvedBy)
    }

    @Test
    fun `columnDelta tolerates a uniform column shift`() {
        val res = resolve(
            result(startLine = 37, startColumn = 31),
            result(startLine = 37, startColumn = 35),
            result(startLine = 37, startColumn = 62),
            result(startLine = 37, startColumn = 75),
        )!!
        assertEquals("columnDelta", res.resolvedBy)
        assertEquals(35, res.result.locations.first().physicalLocation.region.startColumn)
    }

    @Test
    fun `chain falls through filters until one resolves`() {
        // Baseline has no contextSnippet or snippet — chain skips those filters.
        // It DOES have a line/column, so lineDelta (or columnDelta) picks the closest candidate.
        val res = resolve(
            result(startLine = 100, startColumn = 5),
            result(startLine = 110, startColumn = 0),
            result(startLine = 105, startColumn = 0),
        )!!
        assertEquals("lineDelta", res.resolvedBy)
        assertEquals(105, res.result.locations.first().physicalLocation.region.startLine)
    }

    @Test
    fun `chain falls through to fallback when no filter can discriminate`() {
        // All candidates are indistinguishable on every filter.
        val res = resolve(result(), result(), result())!!
        assertEquals("fallback", res.resolvedBy)
    }

    @Test
    fun `score ranks a content match above mere line proximity`() {
        val baseline = result(contextSnippet = "a\nb\nPROBLEM\nc\nd", startLine = 100)
        val contentMatch = result(contextSnippet = "a\nb\nPROBLEM\nc\nd", startLine = 900) // far line, same context
        val lineMatch = result(contextSnippet = "x\ny\nOTHER\nz\nw", startLine = 101)      // close line, other context

        assertTrue(
            TiebreakerCascade.score(baseline, contentMatch) > TiebreakerCascade.score(baseline, lineMatch),
            "exact contextSnippet must outrank line proximity",
        )
    }

    @Test
    fun `null physical location and properties do not crash`() {
        val bare = Result(Message().withText("msg"))
        val res = resolve(bare, bare, Result(Message().withText("msg")))
        assertNotNull(res)
    }

    @Test
    fun `filter is skipped when baseline does not have its signal`() {
        // Baseline has no snippet — `snippet` filter is skipped entirely (does not eliminate candidates).
        // Both candidates have snippets but are otherwise tied; chain resorts to fallback.
        val res = resolve(
            baseline = result(),
            result(snippet = "a"),
            result(snippet = "b"),
        )!!
        assertEquals("fallback", res.resolvedBy)
    }
}
