package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList

class ExtendedFingerprintIntegrationTest {

    private val includeAbsent = BaselineCalculation.Options(true)

    private fun result(
        ruleId: String = "GoPreferNilSlice",
        message: String = "Empty slice declaration using a literal",
        filePath: String = "daemon/libnetwork/ipams/defaultipam/allocator_test.go",
        equalIndicator: Map<Int, String> = emptyMap(),
        extendedFingerprints: Map<Int, String> = emptyMap(),
        snippet: String? = null,
        contextSnippet: String? = null,
        startLine: Int? = null
    ): Result {
        val r = Result(Message().withText(message)).withRuleId(ruleId)

        if (equalIndicator.isNotEmpty()) {
            val vm = VersionedMap<String>()
            for ((v, hash) in equalIndicator) vm.put(BaselineCalculation.EQUAL_INDICATOR, v, hash)
            r.withPartialFingerprints(vm)
        }
        if (extendedFingerprints.isNotEmpty()) {
            val vm = VersionedMap<String>()
            for ((v, hash) in extendedFingerprints) vm.put(BaselineCalculation.EXTENDED_FINGERPRINT, v, hash)
            r.withExtendedFingerprints(vm)
        }

        val region = Region()
        if (snippet != null) region.withSnippet(ArtifactContent().withText(snippet))
        if (startLine != null) region.withStartLine(startLine)
        val physical = PhysicalLocation().withRegion(region)
            .withArtifactLocation(ArtifactLocation().withUri(filePath))
        if (contextSnippet != null) {
            physical.withContextRegion(Region().withSnippet(ArtifactContent().withText(contextSnippet)))
        }
        r.withLocations(listOf(Location().withPhysicalLocation(physical)))

        return r
    }

    private fun report(vararg results: Result): SarifReport =
        SarifReport().withRuns(singletonList(
            Run().withTool(Tool().withDriver(ToolComponent().withName("qodana")))
                .withResults(results.toMutableList())
        ))

    private fun compare(report: SarifReport, baseline: SarifReport) =
        BaselineCalculation.compare(report, baseline, includeAbsent)

    private fun Result.matchedBy(): String? = properties?.get("matchedBy") as? String


    @Test
    fun `equalIndicator wins over resultKey and extendedFingerprint`() {
        val r = result(equalIndicator = mapOf(2 to "5be4eef1f9330d2d"), extendedFingerprints = mapOf(4 to "19b58d31"))
        val b = result(equalIndicator = mapOf(2 to "5be4eef1f9330d2d"), extendedFingerprints = mapOf(4 to "19b58d31"))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator", r.matchedBy())
    }

    @Test
    fun `equalIndicator collisions preserve distinct report rows`() {
        val r1 = result(equalIndicator = mapOf(2 to "H"), message = "M1")
        val r2 = result(equalIndicator = mapOf(2 to "H"), message = "M2")
        val r3 = result(equalIndicator = mapOf(2 to "H"), message = "M3")
        val b1 = result(equalIndicator = mapOf(2 to "H"), message = "B1")
        val b2 = result(equalIndicator = mapOf(2 to "H"), message = "B2")
        val b3 = result(equalIndicator = mapOf(2 to "H"), message = "B3")

        val rep = report(r1, r2, r3)
        val calc = compare(rep, report(b1, b2, b3))

        assertEquals(3, calc.unchangedResults)
        assertEquals(0, calc.newResults)
        assertEquals(0, calc.absentResults)

        val emitted = rep.runs[0].results
        assertEquals(setOf("M1", "M2", "M3"), emitted.map { it.message?.text }.toSet())
        emitted.forEach { assertEquals("equalIndicator", it.matchedBy()) }
    }

    @Test
    fun `extendedFingerprint wins over resultKey`() {
        val r = result(extendedFingerprints = mapOf(4 to "19b58d31"))
        val b = result(extendedFingerprints = mapOf(4 to "19b58d31"))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("extendedFingerprint/v4", r.matchedBy())
    }

    @Test
    fun `extendedFingerprint matches when equalIndicator and resultKey both differ`() {
        val r = result(
            equalIndicator = mapOf(2 to "new_fp"), extendedFingerprints = mapOf(4 to "19b58d31"),
            message = "Refactored message", filePath = "daemon/renamed/allocator_test.go"
        )
        val b = result(
            equalIndicator = mapOf(2 to "old_fp"), extendedFingerprints = mapOf(4 to "19b58d31"),
            message = "Empty slice declaration using a literal", filePath = "daemon/libnetwork/ipams/defaultipam/allocator_test.go"
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(0, calc.newResults)
        assertEquals(0, calc.absentResults)
        assertEquals("extendedFingerprint/v4", r.matchedBy())
    }

    @Test
    fun `higher tier consumed before lower tier`() {
        val r = result(
            equalIndicator = mapOf(2 to "new"), extendedFingerprints = mapOf(4 to "v4hash", 1 to "v1hash"),
            message = "changed"
        )
        val b = result(
            equalIndicator = mapOf(2 to "old"), extendedFingerprints = mapOf(4 to "v4hash", 1 to "v1hash"),
            message = "original"
        )

        compare(report(r), report(b))

        assertEquals("extendedFingerprint/v4", r.matchedBy())
    }

    @Test
    fun `falls to lower tier when higher tier hash differs`() {
        val r = result(
            equalIndicator = mapOf(2 to "new"), extendedFingerprints = mapOf(4 to "different", 2 to "bc92c768"),
            message = "changed"
        )
        val b = result(
            equalIndicator = mapOf(2 to "old"), extendedFingerprints = mapOf(4 to "original", 2 to "bc92c768"),
            message = "original"
        )

        compare(report(r), report(b))

        assertEquals("extendedFingerprint/v2", r.matchedBy())
    }


    @Test
    fun `ambiguous match resolved by context snippet`() {
        val ctx = "\t\tref int\n\t}\n\tips := []IP{}\n\tallocatedIPs := []*net.IPNet{}\n"
        val b = result(
            equalIndicator = mapOf(2 to "old"), extendedFingerprints = mapOf(1 to "bc92c768"),
            snippet = "[]IP", contextSnippet = ctx, message = "original", filePath = "src/old.go"
        )
        val r1 = result(
            equalIndicator = mapOf(2 to "new1"), extendedFingerprints = mapOf(1 to "bc92c768"),
            snippet = "[]IP", contextSnippet = ctx, message = "changed1", filePath = "src/new.go"
        )
        val r2 = result(
            equalIndicator = mapOf(2 to "new2"), extendedFingerprints = mapOf(1 to "bc92c768"),
            snippet = "[]IP", contextSnippet = "different context\n", message = "changed2", filePath = "src/new.go"
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals("extendedFingerprint/v1+contextSnippet", r1.matchedBy())
    }

    @Test
    fun `ambiguous match resolved by snippet when context unavailable`() {
        val b = result(
            equalIndicator = mapOf(2 to "old"), extendedFingerprints = mapOf(3 to "4194b927"),
            snippet = "[]IP", message = "original", filePath = "src/old.go"
        )
        val r1 = result(
            equalIndicator = mapOf(2 to "new1"), extendedFingerprints = mapOf(3 to "4194b927"),
            snippet = "[]IP", message = "changed1", filePath = "src/new.go"
        )
        val r2 = result(
            equalIndicator = mapOf(2 to "new2"), extendedFingerprints = mapOf(3 to "4194b927"),
            snippet = "[]*net.IPNet", message = "changed2", filePath = "src/new.go"
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals("extendedFingerprint/v3+snippet", r1.matchedBy())
    }

    @Test
    fun `ambiguous match resolved by path similarity`() {
        val b = result(
            equalIndicator = mapOf(2 to "old"), extendedFingerprints = mapOf(2 to "32234361"),
            message = "original", filePath = "daemon/libnetwork/ipams/defaultipam/allocator_test.go"
        )
        val r1 = result(
            equalIndicator = mapOf(2 to "new1"), extendedFingerprints = mapOf(2 to "32234361"),
            message = "changed1", filePath = "daemon/libnetwork/ipams/defaultipam/allocator_test.go"
        )
        val r2 = result(
            equalIndicator = mapOf(2 to "new2"), extendedFingerprints = mapOf(2 to "32234361"),
            message = "changed2", filePath = "pkg/completely/different/allocator_test.go"
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals("extendedFingerprint/v2+pathSimilarity", r1.matchedBy())
    }

    @Test
    fun `no match at any stage falls through to new and absent`() {
        val r = result(
            equalIndicator = mapOf(2 to "report_fp"), extendedFingerprints = mapOf(4 to "report_ext"),
            message = "report msg"
        )
        val b = result(
            equalIndicator = mapOf(2 to "baseline_fp"), extendedFingerprints = mapOf(4 to "baseline_ext"),
            message = "baseline msg"
        )

        val calc = compare(report(r), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)
    }

    @Test
    fun `each result matched by the strongest available stage`() {
        // Matched by equalIndicator
        val r1 = result(equalIndicator = mapOf(2 to "5be4eef1"), message = "m1")
        val b1 = result(equalIndicator = mapOf(2 to "5be4eef1"), message = "m1")

        // Matched by resultKey (same ruleId + message + location, no fingerprints)
        val r2 = result(message = "m2", filePath = "src/Same.go")
        val b2 = result(message = "m2", filePath = "src/Same.go")

        // Matched by extendedFingerprint/v3 (different equalIndicator and resultKey)
        val r3 = result(
            equalIndicator = mapOf(2 to "new_fp"), extendedFingerprints = mapOf(3 to "4194b927"),
            message = "refactored", filePath = "src/moved.go"
        )
        val b3 = result(
            equalIndicator = mapOf(2 to "old_fp"), extendedFingerprints = mapOf(3 to "4194b927"),
            message = "original", filePath = "src/old.go"
        )

        val calc = compare(report(r1, r2, r3), report(b1, b2, b3))

        assertEquals(3, calc.unchangedResults)
        assertEquals(0, calc.newResults)
        assertEquals(0, calc.absentResults)
        assertEquals("equalIndicator", r1.matchedBy())
        assertEquals("resultKey", r2.matchedBy())
        assertEquals("extendedFingerprint/v3", r3.matchedBy())
    }
}
