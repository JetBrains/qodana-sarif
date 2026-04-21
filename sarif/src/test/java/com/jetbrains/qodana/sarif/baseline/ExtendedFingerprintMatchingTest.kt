package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options
import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtendedFingerprintMatchingTest {

    private fun result(
        extendedFingerprints: Map<Int, String> = emptyMap(),
        snippet: String? = null,
        contextSnippet: String? = null,
        filePath: String? = null,
        startLine: Int? = null
    ): Result {
        val r = Result(Message().withText("Empty slice declaration using a literal"))
        if (extendedFingerprints.isNotEmpty()) {
            val vm = VersionedMap<String>()
            for ((version, hash) in extendedFingerprints) {
                vm.put(BaselineCalculation.EXTENDED_FINGERPRINT, version, hash)
            }
            r.withExtendedFingerprints(vm)
        }
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

    private fun state() = DiffState(Options(true, true, true))

    private fun match(
        reportResults: List<Result>,
        baselineResults: List<Result>
    ): Triple<DiffState, IdentitySet<Result>, MutableList<Result>> {
        val report = IdentitySet<Result>(reportResults.size).apply { addAll(reportResults) }
        val baseline = baselineResults.toMutableList()
        val s = state()
        ExtendedFingerprintMatching.match(report, baseline, s)
        return Triple(s, report, baseline)
    }

    private fun Result.matchedBy(): String? = properties?.get("matchedBy") as? String

    @Test
    fun `no fingerprints is noop`() {
        val (s, report, baseline) = match(
            listOf(Result(Message().withText("msg"))),
            listOf(Result(Message().withText("msg")))
        )
        assertEquals(1, report.size)
        assertEquals(1, baseline.size)
        assertEquals(0, s.unchanged)
    }

    @Test
    fun `empty collections is noop`() {
        val (s) = match(emptyList(), emptyList())
        assertEquals(0, s.unchanged)
    }

    @Test
    fun `unique match on v4`() {
        val r = result(extendedFingerprints = mapOf(4 to "19b58d31f9bada6c"))
        val (s, report, baseline) = match(listOf(r), listOf(result(extendedFingerprints = mapOf(4 to "19b58d31f9bada6c"))))

        assertEquals(0, report.size)
        assertEquals(0, baseline.size)
        assertEquals(1, s.unchanged)
        assertEquals("extendedFingerprint/v4", r.matchedBy())
    }

    @Test
    fun `different hashes do not match`() {
        val (s, report, baseline) = match(
            listOf(result(extendedFingerprints = mapOf(4 to "19b58d31"))),
            listOf(result(extendedFingerprints = mapOf(4 to "bc92c768")))
        )
        assertEquals(1, report.size)
        assertEquals(1, baseline.size)
        assertEquals(0, s.unchanged)
    }

    @Test
    fun `higher tier consumed before lower tier`() {
        val r = result(extendedFingerprints = mapOf(4 to "19b58d31", 1 to "bc92c768"))
        match(listOf(r), listOf(result(extendedFingerprints = mapOf(4 to "19b58d31", 1 to "bc92c768"))))

        assertEquals("extendedFingerprint/v4", r.matchedBy())
    }

    @Test
    fun `falls to lower tier when higher tier differs`() {
        val r = result(extendedFingerprints = mapOf(4 to "different", 2 to "32234361dd1374d9"))
        match(listOf(r), listOf(result(extendedFingerprints = mapOf(4 to "original", 2 to "32234361dd1374d9"))))

        assertEquals("extendedFingerprint/v2", r.matchedBy())
    }

    @Test
    fun `ambiguous match resolved by context snippet`() {
        val ctx = "\t\tref int\n\t}\n\tips := []IP{}\n\tallocatedIPs := []*net.IPNet{}\n"
        val b = result(extendedFingerprints = mapOf(3 to "4194b927"), snippet = "[]IP", contextSnippet = ctx)
        val r1 = result(extendedFingerprints = mapOf(3 to "4194b927"), snippet = "[]IP", contextSnippet = ctx)
        val r2 = result(extendedFingerprints = mapOf(3 to "4194b927"), snippet = "[]IP", contextSnippet = "other context\n")

        val (s, report) = match(listOf(r1, r2), listOf(b))

        assertEquals(1, s.unchanged)
        assertEquals(1, report.size)
        assertEquals("extendedFingerprint/v3+contextSnippet", r1.matchedBy())
    }

    @Test
    fun `ambiguous match resolved by snippet`() {
        val b = result(extendedFingerprints = mapOf(3 to "4194b927"), snippet = "[]IP")
        val r1 = result(extendedFingerprints = mapOf(3 to "4194b927"), snippet = "[]IP")
        val r2 = result(extendedFingerprints = mapOf(3 to "4194b927"), snippet = "[]*net.IPNet")

        val (s) = match(listOf(r1, r2), listOf(b))

        assertEquals(1, s.unchanged)
        assertEquals("extendedFingerprint/v3+snippet", r1.matchedBy())
    }

    @Test
    fun `ambiguous match resolved by path similarity`() {
        val b = result(extendedFingerprints = mapOf(2 to "32234361"), filePath = "daemon/ipams/allocator_test.go")
        val r1 = result(extendedFingerprints = mapOf(2 to "32234361"), filePath = "daemon/ipams/allocator_test.go")
        val r2 = result(extendedFingerprints = mapOf(2 to "32234361"), filePath = "pkg/completely/different.go")

        val (s) = match(listOf(r1, r2), listOf(b))

        assertEquals(1, s.unchanged)
        assertEquals("extendedFingerprint/v2+pathSimilarity", r1.matchedBy())
    }

    @Test
    fun `ambiguous match resolved by line delta`() {
        val b = result(extendedFingerprints = mapOf(1 to "bc92c768"), startLine = 1224)
        val r1 = result(extendedFingerprints = mapOf(1 to "bc92c768"), startLine = 1226)
        val r2 = result(extendedFingerprints = mapOf(1 to "bc92c768"), startLine = 1500)

        val (s) = match(listOf(r1, r2), listOf(b))

        assertEquals(1, s.unchanged)
        assertEquals("extendedFingerprint/v1+lineDelta", r1.matchedBy())
    }
}
