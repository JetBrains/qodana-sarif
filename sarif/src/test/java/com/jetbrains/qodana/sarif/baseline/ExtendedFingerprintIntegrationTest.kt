package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.MOVE_AND_REFACTOR_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SCOPE_AND_SHAPE_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SHIFT_TOLERANT_INDICATOR
import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.Collections.singletonList

/**
 * End-to-end tests for the baseline matching pipeline.
 *
 * The pipeline iterates over report problems (the anchor) and ranks baseline candidates against
 * each, running matchers in batch-per-phase order:
 *   Phase 1: HashMatcher(equalIndicator)                       — exact, collision-free fingerprint match
 *   Phase 2: HashMatcher(shiftTolerantEqualIndicator)          — content equality
 *   Phase 3: HashMatcher(moveAndRefactorTolerantIndicator)
 *   Phase 4: HashMatcher(extractionAndRefactorTolerantIndicator)  — only across different files; skipped when funcName is absent.
 *   Phase 5: HashMatcher(scopeAndShapeTolerantIndicator)       — broadest key, tried last.
 * Phases 3-5 commit a pair only if its context-snippet similarity reaches the similarity floor.
 */
class ExtendedFingerprintIntegrationTest {

    // matchedMethod is a debug-only switch; these tests assert it, so it defaults on and is toggled per comparison.
    private var includeMatchedMethod = true

    private val options get() = BaselineCalculation.Options(true, includeMatchedMethod)

    /** Every result gets a unique equalIndicator id (as in real reports); bumped per [result] call. */
    private var autoEqualIndicator = 0

    private fun result(
        ruleId: String = "RuleX",
        message: String = "default message",
        filePath: String = "src/example.kt",
        fingerprints: Map<String, Map<Int, String>> = emptyMap(),
        snippet: String? = null,
        contextSnippet: String? = null,
        contextStartLine: Int? = null,
        startLine: Int? = null,
        startColumn: Int? = null,
    ): Result {
        val r = Result(Message().withText(message)).withRuleId(ruleId)

        val vm = VersionedMap<String>()
        for ((key, versions) in fingerprints) {
            for ((v, hash) in versions) vm.put(key, v, hash)
        }
        // Every result needs a unique equalIndicator v1 (the pool id, as in real reports)
        // Fill a fresh one, when a test didn't set v1 itself
        if (vm.get(EQUAL_INDICATOR, 1) == null) vm.put(EQUAL_INDICATOR, 1, "auto-eq-${autoEqualIndicator++}")
        r.withPartialFingerprints(vm)

        val region = Region()
        if (snippet != null) region.withSnippet(ArtifactContent().withText(snippet))
        if (startLine != null) region.withStartLine(startLine)
        if (startColumn != null) region.withStartColumn(startColumn)
        val physical = PhysicalLocation().withRegion(region)
            .withArtifactLocation(ArtifactLocation().withUri(filePath))
        if (contextSnippet != null) {
            val contextRegion = Region().withSnippet(ArtifactContent().withText(contextSnippet))
            if (contextStartLine != null) contextRegion.withStartLine(contextStartLine)
            physical.withContextRegion(contextRegion)
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
        BaselineCalculation.compare(report, baseline, options)

    private fun Result.matchedMethod(): String? = properties?.get("matchedMethod") as? String
    private fun Result.matchedBaselineFingerprint(): String? = properties?.get("matchedBaselineFingerprint") as? String
    private fun Result.cloudId(): Any? = properties?.get("cloudId")

    /** Attaches the cloud-assigned id that a baseline problem carries in a cloud-backed baseline. */
    private fun Result.withCloudId(id: Any): Result = withUpdatedProperties { it["cloudId"] = id }

    @Test
    fun `equalIndicator matches when fingerprints are identical`() {
        val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "fpv1", 2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "fpv1", 2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        // Matched at v2, but the recorded id is the unique v1 one.
        assertEquals("equalIndicator/v2", r.matchedMethod())
        assertEquals("fpv1", r.matchedBaselineFingerprint())
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
        assertEquals("equalIndicator/v2", r.matchedMethod())
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
        assertEquals("shiftTolerantEqualIndicator/v1", r.matchedMethod())
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
        assertEquals("shiftTolerantEqualIndicator/v1", r.matchedMethod())
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
        assertEquals("shiftTolerantEqualIndicator/v1+contextSnippetSimilarity", r.matchedMethod())
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
        assertEquals("moveAndRefactorTolerantIndicator/v1", r.matchedMethod())
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
        assertEquals("extractionAndRefactorTolerantIndicator/v1", r.matchedMethod())
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
        assertEquals("moveAndRefactorTolerantIndicator/v1+lineDelta", r.matchedMethod())
    }

    @Test
    fun `a report whose best candidate was stolen still gets the signal that won its remaining match`() {
        val ctx = "a\nb\nPROBLEM\nc\nd"
        val r1 = result(message = "r1", filePath = "src/f.kt", contextSnippet = ctx, startLine = 50,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr1"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        val r2 = result(message = "r2", filePath = "src/f.kt", startLine = 48,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr2"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

        val b1 = result(message = "b1", filePath = "src/f.kt", contextSnippet = ctx, startLine = 50,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eqb1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb1"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        val b2 = result(message = "b2", filePath = "src/f.kt", startLine = 40,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eqb2"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb2"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        val b3 = result(message = "b3", filePath = "src/f.kt", startLine = 100,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eqb3"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb3"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

        val calc = compare(report(r1, r2), report(b1, b2, b3))

        assertEquals(2, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals(0, calc.newResults)
        assertEquals("moveAndRefactorTolerantIndicator/v1+contextSnippetSimilarity", r1.matchedMethod())
        assertEquals("moveAndRefactorTolerantIndicator/v1+lineDelta", r2.matchedMethod())
        assertEquals("eqb1", r1.matchedBaselineFingerprint())
        assertEquals("eqb2", r2.matchedBaselineFingerprint())
    }

    /**
     * In a cloud-backed baseline *every* baseline problem carries `cloudId`, and the match inherits that same
     * key, so the id travels with the problem instead of being match metadata:
     *   UNCHANGED — inherits the matched baseline's cloudId
     *   ABSENT    — keeps its own cloudId
     *   NEW       — has no cloudId at all
     */
    @Test
    fun `a cloud baseline gives matched problems a cloudId and leaves new problems without one`() {
        val rMatched = result(message = "kept", filePath = "src/kept.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eq-m"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "st-m")))
        val rNew = result(message = "new", filePath = "src/new.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eq-new"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "st-new")))

        val bMatched = result(message = "kept", filePath = "src/kept.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eq-m"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "st-m")))
            .withCloudId("cloud-kept")
        val bGone = result(message = "gone", filePath = "src/gone.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eq-gone"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "st-gone")))
            .withCloudId("cloud-gone")

        val calc = compare(report(rMatched, rNew), report(bMatched, bGone))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)

        // UNCHANGED: inherits the matched baseline's cloud id under the very same key.
        assertEquals("cloud-kept", rMatched.cloudId())
        // The fingerprint of the matched baseline problem is recorded separately and is unaffected.
        assertEquals("eq-m", rMatched.matchedBaselineFingerprint())

        // ABSENT: keeps its own cloud id.
        assertEquals("cloud-gone", bGone.cloudId())

        // NEW: no cloud id at all.
        assertNull(rNew.cloudId())

        // The two id-carrying states are distinguished by baselineState, not by the property.
        assertEquals(Result.BaselineState.UNCHANGED, rMatched.baselineState)
        assertEquals(Result.BaselineState.ABSENT, bGone.baselineState)
        assertEquals(Result.BaselineState.NEW, rNew.baselineState)
    }

    @Test
    fun `a baseline that is not from the cloud produces no cloudId`() {
        val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "fpv1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "fpv1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("fpv1", r.matchedBaselineFingerprint())
        assertNull(r.cloudId())
    }

    @Test
    fun `each report result gets the cloud id of the baseline it actually matched`() {
        val ctx = "a\nb\nPROBLEM\nc\nd"
        val r1 = result(message = "r1", filePath = "src/f.kt", contextSnippet = ctx, startLine = 50,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr1"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        val r2 = result(message = "r2", filePath = "src/f.kt", startLine = 48,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr2"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

        val b1 = result(message = "b1", filePath = "src/f.kt", contextSnippet = ctx, startLine = 50,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eqb1"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb1"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
            .withCloudId("cloud-b1")
        val b2 = result(message = "b2", filePath = "src/f.kt", startLine = 40,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eqb2"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb2"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
            .withCloudId("cloud-b2")
        val b3 = result(message = "b3", filePath = "src/f.kt", startLine = 100,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "eqb3"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb3"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
            .withCloudId("cloud-b3")

        val calc = compare(report(r1, r2), report(b1, b2, b3))

        assertEquals(2, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        // The cloud id follows the pairing the tiebreaker chose, not the candidate order.
        assertEquals("eqb1", r1.matchedBaselineFingerprint())
        assertEquals("eqb2", r2.matchedBaselineFingerprint())
        assertEquals("cloud-b1", r1.cloudId())
        assertEquals("cloud-b2", r2.cloudId())
        // The unmatched baseline is carried over as ABSENT with its own cloud id intact, not another problem's.
        assertEquals("cloud-b3", b3.cloudId())
    }

    @Test
    fun `matches at the greatest common fingerprint version`() {
        val r = result(fingerprints = mapOf(
            EQUAL_INDICATOR to mapOf(1 to "v1", 2 to "v2"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b = result(fingerprints = mapOf(
            EQUAL_INDICATOR to mapOf(1 to "v1", 2 to "v2"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v2", r.matchedMethod())
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
        assertEquals("equalIndicator/v1", r.matchedMethod())
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
        assertEquals("equalIndicator/v2", r1.matchedMethod())
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
        assertEquals("equalIndicator/v2", r1.matchedMethod())
        assertEquals("shiftTolerantEqualIndicator/v1", r2.matchedMethod())
        assertEquals("moveAndRefactorTolerantIndicator/v1", r3.matchedMethod())
    }

    @Test
    fun `match quality is independent of baseline order`() {
        fun bNear() = result(message = "bn", filePath = "src/file.kt", startLine = 11,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "near"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sbn"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        fun bFar() = result(message = "bf", filePath = "src/file.kt", startLine = 400,
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "far"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sbf"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

        for (baseline in listOf(arrayOf(bNear(), bFar()), arrayOf(bFar(), bNear()))) {
            // The report carries no equalIndicator/shiftTolerant match, so it matches via the cascade.
            val r = result(message = "rm", filePath = "src/file.kt", startLine = 10,
                fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

            val calc = compare(report(r), report(*baseline))

            assertEquals(1, calc.unchangedResults)
            assertEquals(1, calc.absentResults)
            assertEquals("moveAndRefactorTolerantIndicator/v1+lineDelta", r.matchedMethod())
            assertEquals("near", r.matchedBaselineFingerprint())
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
                fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "beq"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))

            val calc = compare(report(*reports), report(b))
            val strong = reports.first { it.message.text == "rs" }
            val weak = reports.first { it.message.text == "rw" }

            assertEquals(1, calc.unchangedResults)
            assertEquals(1, calc.newResults)
            assertEquals("beq", strong.matchedBaselineFingerprint())     // strong report took the baseline
            assertNull(weak.matchedBaselineFingerprint())                // weak report was left NEW, not matched
        }
    }

    @Test
    fun `matchedMethod is written only when includeMatchedMethod is enabled`() {
        fun matchUnder(includeMatchedMethod: Boolean): Result {
            val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            this.includeMatchedMethod = includeMatchedMethod
            compare(report(r), report(b))
            return r
        }

        assertEquals("equalIndicator/v2", matchUnder(includeMatchedMethod = true).matchedMethod())
        assertNull(matchUnder(includeMatchedMethod = false).matchedMethod())
    }

    @Test
    fun `matchedBaselineFingerprint is recorded regardless of includeMatchedMethod`() {
        fun matchUnder(includeMatchedMethod: Boolean): Result {
            val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "fpv1", 2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "fpv1", 2 to "fp"), SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
            this.includeMatchedMethod = includeMatchedMethod
            compare(report(r), report(b))
            return r
        }

        assertEquals("fpv1", matchUnder(includeMatchedMethod = true).matchedBaselineFingerprint())
        assertEquals("fpv1", matchUnder(includeMatchedMethod = false).matchedBaselineFingerprint())
    }

    @Test
    fun `includeMatchedMethod is outcome-neutral and stamps every matched result across phases`() {
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
        fun run(includeMatchedMethod: Boolean): List<Result> {
            val reports = reportResults()
            this.includeMatchedMethod = includeMatchedMethod
            val calc = compare(report(*reports.toTypedArray()), report(*baselineResults().toTypedArray()))
            // Outcome must be identical no matter how the flag is set.
            assertEquals(3, calc.unchangedResults)
            assertEquals(0, calc.newResults)
            assertEquals(0, calc.absentResults)
            return reports
        }

        // Flag on: every matched result is stamped with its phase.
        assertEquals(
            listOf("equalIndicator/v2", "shiftTolerantEqualIndicator/v1", "moveAndRefactorTolerantIndicator/v1"),
            run(includeMatchedMethod = true).map { it.matchedMethod() },
        )
        // Flag off: matching is unchanged, but no matchedMethod is written.
        assertTrue(run(includeMatchedMethod = false).all { it.matchedMethod() == null })
    }

    // --- similarity floor ---

    /** Ten context lines: the first [keep] are shared across calls, the rest are unique to [tag]. */
    private fun context(tag: String, keep: Int): String =
        (0 until 10).joinToString("\n") { if (it < keep) "line$it" else "$tag$it" }

    /** Asserts the pair's context-snippet similarity, which the floor tests below are built around. */
    private fun assertSimilarity(expected: Double, a: Result, b: Result) =
        assertEquals(expected, TiebreakerCascade.contextSnippetSimilarity(ResultFeatures(a), ResultFeatures(b)), PRECISION)

    private fun moveResult(message: String, contextSnippet: String?, shift: String) = result(
        message = message, filePath = "src/f.kt", contextSnippet = contextSnippet,
        fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to shift), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")),
    )

    @Test
    fun `a collision-free moveAndRefactor pair below the similarity floor is not matched`() {
        val r = moveResult("r", context("r", keep = 10), "sr")
        val b = moveResult("b", context("b", keep = 4), "sb")
        assertSimilarity(0.4, r, b)

        val calc = compare(report(r), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)
    }

    @Test
    fun `shiftTolerant applies no similarity floor`() {
        val r = result(contextSnippet = context("r", keep = 0), fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        val b = result(contextSnippet = context("b", keep = 0), fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s")))
        assertSimilarity(0.0, r, b)

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("shiftTolerantEqualIndicator/v1", r.matchedMethod())
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "  \n\t\n"])
    fun `a missing or blank context snippet is an unknown similarity and passes the floor`(unknown: String?) {
        val r = moveResult("r", context("r", keep = 10), "sr")
        val b = moveResult("b", unknown, "sb")

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("moveAndRefactorTolerantIndicator/v1", r.matchedMethod())
    }

    @Test
    fun `a baseline without a context snippet is matched when the only scored rival fails the floor`() {
        // Intentional: the known-low pair is rejected, the unknown pair is not, even though it ranks below.
        val r = moveResult("r", context("r", keep = 10), "sr")
        val known = moveResult("known", context("k", keep = 4), "sk")
        val unknown = moveResult("unknown", null, "su")
        assertSimilarity(0.4, r, known)

        val calc = compare(report(r), report(known, unknown))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals(unknown.partialFingerprints.get(EQUAL_INDICATOR, 1), r.matchedBaselineFingerprint())
    }

    @Test
    fun `a rival that fails the floor does not contest the match or shape its label`() {
        val r = moveResult("r", context("r", keep = 10), "sr")
        val eligible = moveResult("eligible", context("e", keep = 5), "se")
        val ineligible = moveResult("ineligible", context("i", keep = 4), "si")
        assertSimilarity(0.5, r, eligible)
        assertSimilarity(0.4, r, ineligible) // below the floor, yet within the decisive margin of the eligible pair

        val calc = compare(report(r), report(eligible, ineligible))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals(eligible.partialFingerprints.get(EQUAL_INDICATOR, 1), r.matchedBaselineFingerprint())
        assertEquals("moveAndRefactorTolerantIndicator/v1", r.matchedMethod())
    }

    @Test
    fun `a contested report whose every candidate fails the floor stays NEW`() {
        val r = moveResult("r", context("r", keep = 10), "sr")
        val b1 = moveResult("b1", context("x", keep = 4), "s1")
        val b2 = moveResult("b2", context("y", keep = 3), "s2")
        assertSimilarity(0.4, r, b1)
        assertSimilarity(0.3, r, b2)

        val calc = compare(report(r), report(b1, b2))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(2, calc.absentResults)
    }

    @Test
    fun `a report rejected by the floor in one phase is matched by a later phase`() {
        val r = result(message = "r", filePath = "src/f.kt", contextSnippet = context("r", keep = 10),
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h"),
                EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "x")))
        val lowMove = result(message = "low", filePath = "src/f.kt", contextSnippet = context("l", keep = 4),
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sl"), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")))
        val extraction = result(message = "ext", filePath = "src/g.kt", contextSnippet = context("e", keep = 6),
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "se"), EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "x")))
        assertSimilarity(0.4, r, lowMove)
        assertSimilarity(0.6, r, extraction)

        val calc = compare(report(r), report(lowMove, extraction))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals("extractionAndRefactorTolerantIndicator/v1", r.matchedMethod())
        assertEquals(extraction.partialFingerprints.get(EQUAL_INDICATOR, 1), r.matchedBaselineFingerprint())
    }

    @Test
    fun `cascade falls through to scopeAndShape when every earlier hash differs`() {
        val r = result(message = "refactored", filePath = "src/new.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "mA"),
                EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "eA"),
                SCOPE_AND_SHAPE_TOLERANT_INDICATOR to mapOf(1 to "shape"),
            ))
        val b = result(message = "original", filePath = "src/old.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sb"),
                MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "mB"),
                EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "eB"),
                SCOPE_AND_SHAPE_TOLERANT_INDICATOR to mapOf(1 to "shape"),
            ))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("scopeAndShapeTolerantIndicator/v1", r.matchedMethod())
    }

    @Test
    fun `scopeAndShape runs after extractionAndRefactor`() {
        val r = result(message = "r", filePath = "src/new.kt",
            fingerprints = mapOf(
                SHIFT_TOLERANT_INDICATOR to mapOf(1 to "sr"),
                EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "x"),
                SCOPE_AND_SHAPE_TOLERANT_INDICATOR to mapOf(1 to "shape"),
            ))
        // Listed first so that baseline order would favour it if phase order did not.
        val byShape = result(message = "shape", filePath = "src/a.kt",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s1"), SCOPE_AND_SHAPE_TOLERANT_INDICATOR to mapOf(1 to "shape")))
        val byExtraction = result(message = "ext", filePath = "src/b.kt",
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to "s2"), EXTRACTION_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "x")))

        val calc = compare(report(r), report(byShape, byExtraction))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.absentResults)
        assertEquals("extractionAndRefactorTolerantIndicator/v1", r.matchedMethod())
        assertEquals(byExtraction.partialFingerprints.get(EQUAL_INDICATOR, 1), r.matchedBaselineFingerprint())
    }

    @Test
    fun `scopeAndShape applies the similarity floor`() {
        fun shapeResult(message: String, contextSnippet: String, shift: String) = result(
            message = message, filePath = "src/f.kt", contextSnippet = contextSnippet,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to shift), SCOPE_AND_SHAPE_TOLERANT_INDICATOR to mapOf(1 to "shape")),
        )
        val r = shapeResult("r", context("r", keep = 10), "sr")
        val b = shapeResult("b", context("b", keep = 4), "sb")
        assertSimilarity(0.4, r, b)

        val calc = compare(report(r), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)
    }

    /**
     * Identical context, but the problem sits on a different line, so the weighted path applies: problem-line
     * similarity is 2·[sharedTokens]/32 and context similarity 1.0. With 5 shared tokens, 0.8·0.3125 + 0.2·1.0 = 0.45
     * exactly, which evaluates to 0.44999999999999996 and must still pass; with 4 it is 0.4 and must be rejected.
     */
    @ParameterizedTest
    @CsvSource("5, 0.45, true", "4, 0.4, false")
    fun `the floor on the weighted path accepts a pair exactly on it and rejects one below`(
        sharedTokens: Int, expectedSimilarity: Double, matched: Boolean,
    ) {
        val shared = (0 until sharedTokens).joinToString(" ") { "c$it" }
        val ctx = "$shared " + (sharedTokens until 16).joinToString(" ") { "a$it" } + "\n" +
                "$shared " + (sharedTokens until 16).joinToString(" ") { "b$it" }
        fun onLine(message: String, line: Int, shift: String) = result(
            message = message, filePath = "src/f.kt", contextSnippet = ctx, contextStartLine = 1, startLine = line,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to shift), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")),
        )
        val r = onLine("r", line = 1, shift = "sr")
        val b = onLine("b", line = 2, shift = "sb")
        assertSimilarity(expectedSimilarity, r, b)

        val calc = compare(report(r), report(b))

        assertEquals(if (matched) 1 else 0, calc.unchangedResults)
        assertEquals(if (matched) 0 else 1, calc.newResults)
    }

    @Test
    fun `a rewritten problem line is rejected even when its surrounding context is unchanged`() {
        // Whole-snippet overlap alone would be 0.8; the problem line shares no token, so 0.8·0 + 0.2·0.8 = 0.16.
        fun withProblem(message: String, problem: String, shift: String) = result(
            message = message, filePath = "src/f.kt", contextSnippet = "a\nb\n$problem\nc\nd", contextStartLine = 10, startLine = 12,
            fingerprints = mapOf(SHIFT_TOLERANT_INDICATOR to mapOf(1 to shift), MOVE_AND_REFACTOR_TOLERANT_INDICATOR to mapOf(1 to "h")),
        )
        val r = withProblem("r", "val total = items.sum()", "sr")
        val b = withProblem("b", "return cache[key]", "sb")
        assertSimilarity(0.16, r, b)

        val calc = compare(report(r), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)
    }
}
