package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [TiebreakerCascade] in isolation.
 *
 * Filter order (after refactoring): contextSnippet → snippet → astPathSimilarity → lineDelta → columnDelta.
 * If all filters tie or yield nothing, `resolvedBy` is "fallback".
 * If there is a single candidate from the start, `resolvedBy` is null.
 */
class TiebreakerCascadeTest {

    private fun result(
        snippet: String? = null,
        contextSnippet: String? = null,
        astPath: String? = null,
        startLine: Int? = null,
        startColumn: Int? = null,
    ): Result {
        val r = Result(Message().withText("Empty slice declaration using a literal"))
        if (astPath != null) r.updateProperties { it["astPath"] = astPath }

        val region = Region()
        if (snippet != null) region.withSnippet(ArtifactContent().withText(snippet))
        if (startLine != null) region.withStartLine(startLine)
        if (startColumn != null) region.withStartColumn(startColumn)
        val physical = PhysicalLocation().withRegion(region)
        if (contextSnippet != null) {
            physical.withContextRegion(Region().withSnippet(ArtifactContent().withText(contextSnippet)))
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
    fun `contextSnippet runs first and picks the candidate with matching context`() {
        val ctx = "before\nproblem\nafter"
        val res = resolve(
            result(contextSnippet = ctx),
            result(contextSnippet = ctx),
            result(contextSnippet = "different\n"),
        )!!
        assertEquals("contextSnippet", res.resolvedBy)
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
    fun `astPathSimilarity breaks ties when content filters do not apply`() {
        val res = resolve(
            result(astPath = "CLASS/CLASS_BODY/FUN/BLOCK/IF/STMT"),
            result(astPath = "CLASS/CLASS_BODY/FUN/BLOCK/IF/STMT"),
            result(astPath = "completely/unrelated/path"),
        )!!
        assertEquals("astPathSimilarity", res.resolvedBy)
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
    fun `columnDelta runs after lineDelta on same-line candidates`() {
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
        // Baseline has no contextSnippet, snippet, or astPath — chain skips those filters.
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
