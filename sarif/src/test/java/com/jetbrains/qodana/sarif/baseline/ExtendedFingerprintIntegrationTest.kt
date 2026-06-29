package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.MOVE_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SHIFT_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList

/**
 * End-to-end tests for the baseline matching pipeline.
 *
 * The pipeline iterates over report problems (the anchor) and ranks baseline candidates against
 * each, running matchers in batch-per-phase order:
 *   Phase 1: HashMatcher(equalIndicator)                       — exact, collision-free fingerprint match
 *   Phase 2: HashMatcher(shiftTolerantEqualIndicator)          — content equality; falls back to
 *            or ResultKeyMatcher                                  ResultKey when neither side carries the hash
 *   Phase 3: HashMatcher(moveAndRefactorTolerantIndicator)
 *   Phase 4: HashMatcher(extractionAndRefactorTolerantIndicator)  — only across different files; skipped when funcName is absent.
 */
class ExtendedFingerprintIntegrationTest {

    private val includeAbsentAndMatchedBy = BaselineCalculation.Options(true, true)

    private fun result(
        ruleId: String = "RuleX",
        message: String = "default message",
        filePath: String = "src/example.kt",
        fingerprints: Map<String, Map<Int, String>> = emptyMap(),
        snippet: String? = null,
        contextSnippet: String? = null,
        startLine: Int? = null,
        startColumn: Int? = null,
    ): Result {
        val r = Result(Message().withText(message)).withRuleId(ruleId)

        if (fingerprints.isNotEmpty()) {
            val vm = VersionedMap<String>()
            for ((key, versions) in fingerprints) {
                for ((v, hash) in versions) vm.put(key, v, hash)
            }
            r.withPartialFingerprints(vm)
        }

        val region = Region()
        if (snippet != null) region.withSnippet(ArtifactContent().withText(snippet))
        if (startLine != null) region.withStartLine(startLine)
        if (startColumn != null) region.withStartColumn(startColumn)
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
        BaselineCalculation.compare(report, baseline, includeAbsentAndMatchedBy)

    private fun Result.matchedBy(): String? = properties?.get("matchedBy") as? String
    private fun Result.matchedWith(): String? = properties?.get("matchedWith") as? String

    @Test
    fun `equalIndicator matches when fingerprints are identical`() {
        val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v2", r.matchedBy())
        assertEquals("fp", r.matchedWith())
    }

    @Test
    fun `equalIndicator wins over later phases when both could match`() {
        val r = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "fp"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )
        val b = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "fp"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v2", r.matchedBy())
    }

    @Test
    fun `shiftTolerant matches identical content`() {
        val r = result(message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b = result(message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("shiftTolerantEqualIndicator/v1", r.matchedBy())
    }

    @Test
    fun `shiftTolerant wins over hash cascade for content-stable results`() {
        val r = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )
        val b = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("shiftTolerantEqualIndicator/v1", r.matchedBy())
    }

    @Test
    fun `shiftTolerant collisions are resolved by the tiebreaker chain`() {
        // Same shiftTolerant hash on multiple baseline candidates disambiguated against the report anchor
        // by contextSnippet.
        val ctx = "before\nproblem\nafter"
        val r = result(filePath = "src/a.kt", contextSnippet = ctx,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b1 = result(filePath = "src/a.kt", contextSnippet = ctx,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b2 = result(filePath = "src/a.kt", contextSnippet = "different\n",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))

        val calc = compare(report(r), report(b1, b2))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals("shiftTolerantEqualIndicator/v1+contextSnippetSimilarity", r.matchedBy())
    }

    @Test
    fun `cascade catches matches when content changed but structure preserved`() {
        // Different message AND different file: the content phase cannot match.
        // moveAndRefactorTolerantIndicator hashes agree → cascade rescues.
        val r = result(
            message = "refactored", filePath = "src/new.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )
        val b = result(
            message = "original", filePath = "src/old.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("moveAndRefactorTolerantIndicator/v1", r.matchedBy())
    }

    @Test
    fun `cascade falls through to extractionAndRefactor when top tier hash differs`() {
        val r = result(
            message = "refactored", filePath = "src/new.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "diffA"),
                EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "match"),
            ),
        )
        val b = result(
            message = "original", filePath = "src/old.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "diffB"),
                EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "match"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("extractionAndRefactorTolerantIndicator/v1", r.matchedBy())
    }

    @Test
    fun `policy does NOT apply to moveAndRefactor with lineDelta tiebreaker`() {
        val r = result(
            message = "rm", filePath = "src/r.kt", startLine = 10,
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h"),
            ),
        )
        val b1 = result(
            message = "b1", filePath = "src/b1.kt", startLine = 12,
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb1"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h"),
            ),
        )
        val b2 = result(
            message = "b2", filePath = "src/b2.kt", startLine = 200,
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb2"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h"),
            ),
        )

        val calc = compare(report(r), report(b1, b2))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals("moveAndRefactorTolerantIndicator/v1+lineDelta", r.matchedBy())
    }

    @Test
    fun `matches at the greatest common fingerprint version`() {
        val r = result(fingerprints = mapOf(
            EQUAL_INDICATOR to mapOf(1 to "v1", 2 to "v2"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b = result(fingerprints = mapOf(
            EQUAL_INDICATOR to mapOf(1 to "v1", 2 to "v2"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v2", r.matchedBy())
    }

    @Test
    fun `no fallback to lower version when higher common version disagrees`() {
        val r = result(
            filePath = "src/r.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(1 to "shared", 2 to "r"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "mr"),
            ),
        )
        val b = result(
            filePath = "src/b.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(1 to "shared", 2 to "b"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "mb"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)
    }

    @Test
    fun `matches at lower version when higher version is one-sided`() {
        // Baseline has v1 only; report has v1 + v2 → greatest common is v1.
        val r = result(
            filePath = "src/r.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(1 to "shared", 2 to "r_only"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "mr"),
            ),
        )
        val b = result(
            filePath = "src/b.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(1 to "shared"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "mb"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v1", r.matchedBy())
    }

    @Test
    fun `stronger phase wins globally regardless of baseline iteration order`() {
        val b1 = result(
            message = "b1-cascade", filePath = "src/b1.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb1"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )
        val b2 = result(
            message = "b2-eq", filePath = "src/b2.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "eq"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb2"),
            ),
        )
        val r1 = result(
            message = "r1-both", filePath = "src/r1.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "eq"),
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr1"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"),
            ),
        )

        val calc = compare(report(r1), report(b1, b2))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals("equalIndicator/v2", r1.matchedBy())
    }

    @Test
    fun `no match anywhere produces NEW for report and ABSENT for baseline`() {
        val r = result(
            message = "r-msg", filePath = "src/r.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "r_eq"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "r_loc"),
            ),
        )
        val b = result(
            message = "b-msg", filePath = "src/b.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "b_eq"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "b_loc"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)
    }

    @Test
    fun `each result is matched by the strongest available phase`() {
        // matched by equalIndicator
        val r1 = result(message = "m1", filePath = "src/eq.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "eq1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s1")))
        val b1 = result(message = "m1", filePath = "src/eq.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "eq1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s1")))

        // matched by shiftTolerantEqualIndicator (content-stable, no equalIndicator)
        val r2 = result(message = "m2", filePath = "src/rk.kt",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s2")))
        val b2 = result(message = "m2", filePath = "src/rk.kt",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s2")))

        // matched by moveAndRefactorTolerantIndicator (cascade only — content has changed)
        val r3 = result(message = "m3-new", filePath = "src/cs.kt",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s3r"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc")))
        val b3 = result(message = "m3-old", filePath = "src/cs.kt",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s3b"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc")))

        val calc = compare(report(r1, r2, r3), report(b1, b2, b3))

        assertEquals(3, calc.unchangedResults)
        assertEquals(0, calc.newResults)
        assertEquals(0, calc.absentResults)
        assertEquals("equalIndicator/v2", r1.matchedBy())
        assertEquals("shiftTolerantEqualIndicator/v1", r2.matchedBy())
        assertEquals("moveAndRefactorTolerantIndicator/v1", r3.matchedBy())
    }

    @Test
    fun `match quality is independent of baseline order`() {
        fun bNear() = result(message = "bn", filePath = "src/file.kt", startLine = 11,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "near"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sbn"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        fun bFar() = result(message = "bf", filePath = "src/file.kt", startLine = 400,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "far"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sbf"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

        for (baseline in listOf(arrayOf(bNear(), bFar()), arrayOf(bFar(), bNear()))) {
            // The report carries no equalIndicator/shiftTolerant match, so it matches via the cascade.
            val r = result(message = "rm", filePath = "src/file.kt", startLine = 10,
                fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

            val calc = compare(report(r), report(*baseline))

            assertEquals(1, calc.unchangedResults)
            assertEquals(1, calc.absentResults)
            assertEquals("moveAndRefactorTolerantIndicator/v1+lineDelta", r.matchedBy())
            assertEquals("near", r.matchedWith())
        }
    }

    @Test
    fun `match quality is independent of report order`() {
        fun rStrong() = result(message = "rs", filePath = "src/file.kt", startLine = 400,
            contextSnippet = "a\nb\nPROBLEM\nc\nd",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "ss"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        fun rWeak() = result(message = "rw", filePath = "src/file.kt", startLine = 11,
            contextSnippet = "x\ny\nOTHER\nz\nw",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sw"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

        for (reports in listOf(arrayOf(rWeak(), rStrong()), arrayOf(rStrong(), rWeak()))) {
            val b = result(message = "bm", filePath = "src/file.kt", startLine = 10,
                contextSnippet = "a\nb\nPROBLEM\nc\nd",
                fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "beq"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

            val calc = compare(report(*reports), report(b))
            val strong = reports.first { it.message.text == "rs" }
            val weak = reports.first { it.message.text == "rw" }

            assertEquals(1, calc.unchangedResults)
            assertEquals(1, calc.newResults)
            assertEquals("beq", strong.matchedWith())     // strong report took the baseline
            assertNull(weak.matchedWith())                // weak report was left NEW, not matched
        }
    }

    @Test
    fun `matchedBy is written only when includeMatchedBy is enabled`() {
        fun matchUnder(includeMatchedBy: Boolean): Result {
            val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            BaselineCalculation.compare(report(r), report(b), BaselineCalculation.Options(true, includeMatchedBy))
            return r
        }

        assertEquals("equalIndicator/v2", matchUnder(includeMatchedBy = true).matchedBy())
        assertNull(matchUnder(includeMatchedBy = false).matchedBy())
    }

    @Test
    fun `matchedWith is recorded regardless of includeMatchedBy`() {
        fun matchUnder(includeMatchedBy: Boolean): Result {
            val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            BaselineCalculation.compare(report(r), report(b), BaselineCalculation.Options(true, includeMatchedBy))
            return r
        }

        assertEquals("fp", matchUnder(includeMatchedBy = true).matchedWith())
        assertEquals("fp", matchUnder(includeMatchedBy = false).matchedWith())
    }

    @Test
    fun `includeMatchedBy is outcome-neutral and stamps every matched result across phases`() {
        // One match per phase: equalIndicator, shiftTolerant and the move cascade.
        fun reportResults() = listOf(
            result(message = "m1", filePath = "src/eq.kt",
                fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "eq1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s1"))),
            result(message = "m2", filePath = "src/st.kt",
                fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s2"))),
            result(message = "m3-new", filePath = "src/cs.kt",
                fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s3r"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"))),
        )
        fun baselineResults() = listOf(
            result(message = "m1", filePath = "src/eq.kt",
                fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "eq1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s1"))),
            result(message = "m2", filePath = "src/st.kt",
                fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s2"))),
            result(message = "m3-old", filePath = "src/cs.kt",
                fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s3b"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "loc"))),
        )

        // compare mutates results in place, so build fresh inputs for each run.
        fun run(includeMatchedBy: Boolean): List<Result> {
            val reports = reportResults()
            val calc = BaselineCalculation.compare(
                report(*reports.toTypedArray()), report(*baselineResults().toTypedArray()),
                BaselineCalculation.Options(true, includeMatchedBy),
            )
            // Outcome must be identical no matter how the flag is set.
            assertEquals(3, calc.unchangedResults)
            assertEquals(0, calc.newResults)
            assertEquals(0, calc.absentResults)
            return reports
        }

        // Flag on: every matched result is stamped with its phase.
        assertEquals(
            listOf("equalIndicator/v2", "shiftTolerantEqualIndicator/v1", "moveAndRefactorTolerantIndicator/v1"),
            run(includeMatchedBy = true).map { it.matchedBy() },
        )
        // Flag off: matching is unchanged, but no matchedBy is written.
        assertTrue(run(includeMatchedBy = false).all { it.matchedBy() == null })
    }
}
