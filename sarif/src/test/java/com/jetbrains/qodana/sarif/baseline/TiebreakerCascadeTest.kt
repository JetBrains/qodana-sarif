package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TiebreakerCascadeTest {

    private fun result(
        snippet: String? = null,
        contextSnippet: String? = null,
        filePath: String? = null,
        startLine: Int? = null
    ): Result {
        val r = Result(Message().withText("Empty slice declaration using a literal"))
        val region = Region()
        if (snippet != null) region.withSnippet(ArtifactContent().withText(snippet))
        if (startLine != null) region.withStartLine(startLine)
        val physical = PhysicalLocation().withRegion(region)
        if (filePath != null) physical.withArtifactLocation(ArtifactLocation().withUri(filePath))
        if (contextSnippet != null) {
            physical.withContextRegion(Region().withSnippet(ArtifactContent().withText(contextSnippet)))
        }
        r.withLocations(listOf(Location().withPhysicalLocation(physical)))
        return r
    }

    private fun resolve(baseline: Result, vararg candidates: Result) =
        TiebreakerCascade.resolve(baseline, candidates.toList())

    @Test
    fun `empty candidates returns null`() {
        assertNull(resolve(result()))
    }

    @Test
    fun `single candidate returns it without resolvedBy`() {
        val c = result()
        val res = resolve(result(), c)!!
        assertSame(c, res.result)
        assertNull(res.resolvedBy)
    }

    @Test
    fun `context snippet wins first`() {
        val ctx = "\t\tref int\n\t}\n\tips := []IP{}\n"
        val res = resolve(
            result(snippet = "[]IP", contextSnippet = ctx),
            result(snippet = "[]IP", contextSnippet = ctx),
            result(snippet = "[]IP", contextSnippet = "different\n")
        )!!
        assertEquals("contextSnippet", res.resolvedBy)
    }

    @Test
    fun `snippet breaks tie when context unavailable`() {
        val res = resolve(
            result(snippet = "[]IP"),
            result(snippet = "[]IP"),
            result(snippet = "[]*net.IPNet")
        )!!
        assertEquals("snippet", res.resolvedBy)
    }

    @Test
    fun `filename breaks tie before path filters`() {
        val res = resolve(
            result(filePath = "daemon/ipams/allocator_test.go"),
            result(filePath = "pkg/completely/different/allocator_test.go"),
            result(filePath = "pkg/something/other.go")
        )!!
        assertEquals("filename", res.resolvedBy)
    }

    @Test
    fun `path similarity breaks tie when filenames are identical`() {
        val res = resolve(
            result(filePath = "daemon/ipams/allocator_test.go"),
            result(filePath = "daemon/ipams/allocator_test.go"),
            result(filePath = "pkg/completely/different/allocator_test.go")
        )!!
        assertEquals("pathSimilarity", res.resolvedBy)
    }

    @Test
    fun `line delta is the last resort`() {
        val res = resolve(
            result(startLine = 1224),
            result(startLine = 1226),
            result(startLine = 1500)
        )!!
        assertEquals("lineDelta", res.resolvedBy)
    }

    @Test
    fun `returns first candidate when all signals identical`() {
        val res = resolve(result(), result(), result())!!
        assertNull(res.resolvedBy)
    }

    @Test
    fun `null properties and locations do not crash`() {
        val bare = Result(Message().withText("msg"))
        assertNotNull(resolve(bare, bare, Result(Message().withText("msg"))))
    }
}
