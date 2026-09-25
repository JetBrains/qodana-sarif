package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList

/**
 * `cloudId` propagation on the legacy path, the algorithm used when neither report nor baseline carries a
 * shiftTolerantEqualIndicator, so no result here ever gets one.
 *
 * The legacy algorithm decides in two passes, and the cloud id has to follow both:
 *   1. equalIndicator (any version): the baseline problem behind the match is known
 *   2. content equality ([ResultKey]): matched against a *counter* of baseline problems, so the problem behind the
 *      match is never known: the id comes off the counter with the count the match consumes, and only a problem that
 *      the counter then drops without a trace has an id to give
 */
class LegacyAlgCloudBaselineTest {

    /** Every result gets a unique equalIndicator, as in real reports, unless the test pins one itself. */
    private var autoEqualIndicator = 0

    private fun result(
        ruleId: String = "RuleX",
        message: String = "default message",
        filePath: String = "src/example.kt",
        equalIndicators: Map<Int, String> = emptyMap(),
        snippet: String? = null,
        startLine: Int? = null,
    ): Result {
        val r = Result(Message().withText(message)).withRuleId(ruleId)

        val vm = VersionedMap<String>()
        for ((version, hash) in equalIndicators) vm.put(EQUAL_INDICATOR, version, hash)
        if (vm.get(EQUAL_INDICATOR, 1) == null && equalIndicators.isEmpty()) {
            vm.put(EQUAL_INDICATOR, 1, "auto-eq-${autoEqualIndicator++}")
        }
        r.withPartialFingerprints(vm)

        val region = Region()
        if (snippet != null) region.withSnippet(ArtifactContent().withText(snippet))
        if (startLine != null) region.withStartLine(startLine)
        r.withLocations(listOf(Location().withPhysicalLocation(
            PhysicalLocation().withRegion(region).withArtifactLocation(ArtifactLocation().withUri(filePath))
        )))

        return r
    }

    private fun report(vararg results: Result): SarifReport =
        SarifReport().withRuns(singletonList(
            Run().withTool(Tool().withDriver(ToolComponent().withName("qodana")))
                .withResults(results.toMutableList())
        ))

    private fun compare(
        report: SarifReport,
        baseline: SarifReport,
        options: BaselineCalculation.Options = BaselineCalculation.Options(true, false),
    ) = BaselineCalculation.compare(report, baseline, options)

    private fun Result.cloudId(): Any? = properties?.get("cloudId")

    private fun Result.withCloudId(id: Any): Result = withUpdatedProperties { it["cloudId"] = id }

    @Test
    fun `an equalIndicator match inherits the cloud id, absent keeps its own, new has none`() {
        val rMatched = result(message = "kept", filePath = "src/kept.kt", equalIndicators = mapOf(1 to "eq-m"))
        val rNew = result(message = "new", filePath = "src/new.kt", equalIndicators = mapOf(1 to "eq-new"))

        val bMatched = result(message = "kept", filePath = "src/kept.kt", equalIndicators = mapOf(1 to "eq-m"))
            .withCloudId("cloud-kept")
        val bGone = result(message = "gone", filePath = "src/gone.kt", equalIndicators = mapOf(1 to "eq-gone"))
            .withCloudId("cloud-gone")

        val calc = compare(report(rMatched, rNew), report(bMatched, bGone))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)

        assertEquals("cloud-kept", rMatched.cloudId())
        assertEquals("cloud-gone", bGone.cloudId())
        assertNull(rNew.cloudId())

        assertEquals(Result.BaselineState.UNCHANGED, rMatched.baselineState)
        assertEquals(Result.BaselineState.ABSENT, bGone.baselineState)
        assertEquals(Result.BaselineState.NEW, rNew.baselineState)
    }

    @Test
    fun `a match at an older equalIndicator version still inherits the cloud id`() {
        val r = result(equalIndicators = mapOf(1 to "shared-v1", 2 to "report-v2"))
        val b = result(equalIndicators = mapOf(1 to "shared-v1", 2 to "baseline-v2")).withCloudId("cloud-1")

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("cloud-1", r.cloudId())
    }

    @Test
    fun `a baseline that is not from the cloud produces no cloudId`() {
        val r = result(equalIndicators = mapOf(1 to "eq"))
        val b = result(equalIndicators = mapOf(1 to "eq"))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertNull(r.cloudId())
    }

    /**
     * The fingerprint changed, so only content equality can match: the cloud id comes from the baseline problem the
     * content-equality counter consumed.
     */
    @Test
    fun `a content-equality match inherits the cloud id of the problem it replaced`() {
        val r = result(snippet = "boom()", startLine = 12, equalIndicators = mapOf(1 to "eq-new"))
        val b = result(snippet = "boom()", startLine = 10, equalIndicators = mapOf(1 to "eq-old"))
            .withCloudId("cloud-1")

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(0, calc.absentResults)
        assertEquals(0, calc.newResults)
        assertEquals("cloud-1", r.cloudId())
    }

    /** Content-equal problems are interchangeable, so any pairing will do — but no cloud id may be used twice. */
    @Test
    fun `content-equal problems get one distinct cloud id each`() {
        val r1 = result(snippet = "boom()", startLine = 12, equalIndicators = mapOf(1 to "eq-r1"))
        val r2 = result(snippet = "boom()", startLine = 30, equalIndicators = mapOf(1 to "eq-r2"))

        val b1 = result(snippet = "boom()", startLine = 10, equalIndicators = mapOf(1 to "eq-b1"))
            .withCloudId("cloud-1")
        val b2 = result(snippet = "boom()", startLine = 28, equalIndicators = mapOf(1 to "eq-b2"))
            .withCloudId("cloud-2")

        val calc = compare(report(r1, r2), report(b1, b2))

        assertEquals(2, calc.unchangedResults)
        assertEquals(0, calc.absentResults)
        assertEquals(setOf("cloud-1", "cloud-2"), setOf(r1.cloudId(), r2.cloudId()))
    }

    /**
     * With more content-equal baseline problems than report results, the surplus is reported ABSENT under its own
     * cloud id — so the id handed to the report result has to come from the problem that is dropped instead, which is
     * the one the counter runs out on: the last of the group.
     */
    @Test
    fun `the absent problem of a group keeps its cloud id, the dropped one hands its over`() {
        val r = result(snippet = "boom()", startLine = 12, equalIndicators = mapOf(1 to "eq-r"))

        val bAbsent = result(snippet = "boom()", startLine = 10, equalIndicators = mapOf(1 to "eq-b1"))
            .withCloudId("cloud-1")
        val bDropped = result(snippet = "boom()", startLine = 28, equalIndicators = mapOf(1 to "eq-b2"))
            .withCloudId("cloud-2")

        val calc = compare(report(r), report(bAbsent, bDropped))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals(0, calc.newResults)

        // bDropped leaves no trace of its own, so only its id is free
        assertEquals(Result.BaselineState.ABSENT, bAbsent.baselineState)
        assertNull(bDropped.baselineState)
        assertEquals("cloud-1", bAbsent.cloudId())
        assertEquals("cloud-2", r.cloudId())
        // the point of the above: no cloud id may name two results of one report
        assertNotEquals(bAbsent.cloudId(), r.cloudId())
    }

    /**
     * The problem an equalIndicator match already consumed is not up for grabs: the content-equality counter still
     * counts it (that is the legacy matching rule), but its cloud id stays with the result that matched it.
     */
    @Test
    fun `a cloud id claimed by an equalIndicator match is not handed out again`() {
        val rMatched = result(snippet = "boom()", startLine = 12, equalIndicators = mapOf(1 to "eq-shared"))
        val rByContent = result(snippet = "boom()", startLine = 30, equalIndicators = mapOf(1 to "eq-changed"))

        val bMatched = result(snippet = "boom()", startLine = 12, equalIndicators = mapOf(1 to "eq-shared"))
            .withCloudId("cloud-1")
        val bOther = result(snippet = "boom()", startLine = 28, equalIndicators = mapOf(1 to "eq-b2"))
            .withCloudId("cloud-2")

        val calc = compare(report(rMatched, rByContent), report(bMatched, bOther))

        assertEquals(2, calc.unchangedResults)
        assertEquals(1, calc.absentResults)

        assertEquals("cloud-1", rMatched.cloudId())
        // bOther is reported ABSENT with its own id, so there is nothing left for rByContent to inherit.
        assertEquals("cloud-2", bOther.cloudId())
        assertNull(rByContent.cloudId())
    }

    /** An unchecked baseline problem is carried over as-is, cloud id included. */
    @Test
    fun `an unchecked baseline problem keeps its cloud id`() {
        val r = result(message = "new", filePath = "src/new.kt", equalIndicators = mapOf(1 to "eq-new"))
        val bUnchecked = result(message = "out of scope", filePath = "src/skipped.kt", equalIndicators = mapOf(1 to "eq-skip"))
            .withCloudId("cloud-skipped")

        val options = BaselineCalculation.Options(true, true, true) { false }
        val calc = compare(report(r), report(bUnchecked), options)

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(0, calc.absentResults)

        assertEquals(Result.BaselineState.UNCHANGED, bUnchecked.baselineState)
        assertEquals("cloud-skipped", bUnchecked.cloudId())
        assertNull(r.cloudId())
    }

    @Test
    fun `no cloud id is written when unchanged results are excluded`() {
        val r = result(equalIndicators = mapOf(1 to "eq"))
        val b = result(equalIndicators = mapOf(1 to "eq")).withCloudId("cloud-1")

        compare(report(r), report(b), BaselineCalculation.Options(false, false, false, true))

        assertNull(r.cloudId())
    }
}
