package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.SarifUtil
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.BASELINE_ORIGIN
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.BASELINE_SOURCE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * The report's `baselineSource` follows the baseline file's `baselineOrigin`: CLOUD only when the origin says so,
 * LOCAL for any other baseline, and absent when no baseline run was applied.
 */
class BaselineSourceTest {

    private var autoEqualIndicator = 0

    private fun result(): Result =
        Result(Message().withText("message")).withRuleId("RuleX")
            .withPartialFingerprints(VersionedMap<String>().apply { put(EQUAL_INDICATOR, 1, "eq-${autoEqualIndicator++}") })

    private fun run(toolName: String = "qodana", origin: String? = null): Run =
        Run().withTool(Tool().withDriver(ToolComponent().withName(toolName)))
            .withResults(mutableListOf(result()))
            .apply { if (origin != null) withUpdatedProperties { it[BASELINE_ORIGIN] = origin } }

    private fun report(vararg runs: Run): SarifReport = SarifReport().withRuns(runs.toMutableList())

    private fun Run.baselineSource(): Any? = properties?.get(BASELINE_SOURCE)

    @Test
    fun `a cloud baseline makes the source CLOUD`() {
        val r = run()
        BaselineCalculation.compare(report(r), report(run(origin = "CLOUD")))

        assertEquals("CLOUD", r.baselineSource())
    }

    @Test
    fun `a baseline without an origin makes the source LOCAL`() {
        val r = run()
        BaselineCalculation.compare(report(r), report(run()))

        assertEquals("LOCAL", r.baselineSource())
    }

    @Test
    fun `an explicit LOCAL origin makes the source LOCAL`() {
        val r = run()
        BaselineCalculation.compare(report(r), report(run(origin = "LOCAL")))

        assertEquals("LOCAL", r.baselineSource())
    }

    /** A manually merged baseline: each report run takes the source from the origin of its own baseline run. */
    @Test
    fun `each run of a merged baseline keeps its own origin`() {
        val fromCloud = run(toolName = "qodana")
        val fromLocal = run(toolName = "other")
        BaselineCalculation.compare(
            report(fromCloud, fromLocal),
            report(run(toolName = "qodana", origin = "CLOUD"), run(toolName = "other", origin = "LOCAL")),
        )

        assertEquals("CLOUD", fromCloud.baselineSource())
        assertEquals("LOCAL", fromLocal.baselineSource())
    }

    @Test
    fun `an unknown origin is not trusted as cloud`() {
        val r = run()
        BaselineCalculation.compare(report(r), report(run(origin = "SOMETHING")))

        assertEquals("LOCAL", r.baselineSource())
    }

    /** A report compared with a cloud baseline and reused as the next baseline is a local one: no chaining of CLOUD. */
    @Test
    fun `a previous report reused as the baseline is LOCAL`() {
        val first = run()
        BaselineCalculation.compare(report(first), report(run(origin = "CLOUD")))
        assertEquals("CLOUD", first.baselineSource())

        val second = run()
        BaselineCalculation.compare(report(second), report(first))

        assertEquals("LOCAL", second.baselineSource())
        assertNull(second.properties?.get(BASELINE_ORIGIN))
    }

    @Test
    fun `a report run without a baseline run has no source`() {
        val matched = run(toolName = "qodana")
        val unmatched = run(toolName = "other")
        BaselineCalculation.compare(report(matched, unmatched), report(run(toolName = "qodana", origin = "CLOUD")))

        assertEquals("CLOUD", matched.baselineSource())
        assertNull(unmatched.baselineSource())
    }

    @Test
    fun `existing run properties are kept`() {
        val r = run().withUpdatedProperties { it["other"] = "value" }
        BaselineCalculation.compare(report(r), report(run(origin = "CLOUD")))

        assertEquals("value", r.properties?.get("other"))
        assertEquals("CLOUD", r.baselineSource())
    }

    @Test
    fun `the origin is read from a deserialized baseline file`() {
        val baseline = SarifUtil.readReport(StringReader(
            """
            {
              "version": "2.1.0",
              "runs": [
                {
                  "tool": { "driver": { "name": "qodana" } },
                  "properties": { "baselineOrigin": "CLOUD" },
                  "results": []
                }
              ]
            }
            """.trimIndent()
        ))
        val r = run()
        BaselineCalculation.compare(report(r), baseline)

        assertEquals("CLOUD", r.baselineSource())
    }
}
