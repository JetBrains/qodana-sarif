package com.jetbrains.qodana.sarif

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation
import com.jetbrains.qodana.sarif.model.Result.BaselineState
import com.jetbrains.qodana.sarif.model.SarifReport
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.nio.file.Paths
import kotlin.streams.asStream

class BaselineFingerprintTest {
    private fun read(path: String) = SarifUtil.readReport(Paths.get(path))

    // The findings in both reports are the same except for the human-readable message
    private fun readBaseline(fingerprintVersion: String) =
        read("src/test/resources/testData/fingerprinting/before.sarif_$fingerprintVersion.json")

    private fun readReport(fingerprintVersion: String) =
        read("src/test/resources/testData/fingerprinting/after.sarif_$fingerprintVersion.json")

    private fun test(
        report: SarifReport,
        baseline: SarifReport,
        expect: Map<BaselineState, Int>,
    ) {
        val res = BaselineCalculation.compare(report, baseline, BaselineCalculation.Options(true))

        val byState = report.runs.orEmpty().asSequence()
            .flatMap { it.results.orEmpty() }
            .groupingBy { it.baselineState }
            .eachCount()

        sequenceOf(BaselineState.UNCHANGED, BaselineState.ABSENT, BaselineState.NEW)
            .flatMap {
                val inReport = byState[it] ?: 0
                val inResult = when (it) {
                    BaselineState.NEW -> res.newResults
                    BaselineState.UNCHANGED -> res.unchangedResults
                    BaselineState.ABSENT -> res.absentResults
                    BaselineState.UPDATED -> error("Not in source")
                }
                val expected = expect[it] ?: 0
                sequenceOf(
                    Executable { Assertions.assertEquals(expected, inReport, "[$it] in report") },
                    Executable { Assertions.assertEquals(expected, inResult, "[$it] in result") },
                )
            }
            .asStream()
            .let(Assertions::assertAll)
    }

    private val expectBaselineMismatch = mapOf(
        BaselineState.UNCHANGED to 1,
        BaselineState.ABSENT to 17,
        BaselineState.NEW to 17,
    )

    private val expectBaselineMatch = mapOf(BaselineState.UNCHANGED to 18)

    @Test
    fun `both are v1`() {
        // V1 fingerprint checks message => results are all absent / new
        test(readReport("v1"), readBaseline("v1"), expectBaselineMismatch)
    }

    @Test
    fun `both are v2`() {
        // V2 fingerprint ignores message => all results are unchanged
        test(readReport("v2"), readBaseline("v2"), expectBaselineMatch)
    }

    @Test
    fun `backward compat -- report v1_v2 - baseline v1`() {
        // we can only compare v1 because baseline doesn't have v2
        test(readReport("v1_v2"), readBaseline("v1"), expectBaselineMismatch)
    }

    @Test
    fun `forward compat -- report v1 - baseline v1_v2`() {
        // we can only compare v1 because report doesn't have v2
        test(readReport("v1"), readBaseline("v1_v2"), expectBaselineMismatch)
    }

    @Test
    fun `v1_v2 to v1_v2`() {
        // while V1 differs, V2 is the same, so expect match
        test(readReport("v1_v2"), readBaseline("v1_v2"), expectBaselineMatch)
    }
}
