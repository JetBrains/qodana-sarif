package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.EQUAL_INDICATOR
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_FUNC_AND_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_LOCATION_AND_SHAPE
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation.SAME_SHAPE
import com.jetbrains.qodana.sarif.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList

/**
 * End-to-end tests for the baseline matching pipeline.
 *
 * The pipeline runs three matchers in batch-per-phase order:
 *   Phase 1: EqualIndicatorMatcher  — exact fingerprint match
 *   Phase 2: ResultKeyMatcher       — exact content match (ruleId + message + URI + snippet + ...)
 *   Phase 3: HashCascadeMatcher     — structural cascade (sameLocationAndShape → sameFuncAndShape → sameShape),
 *                                     with a policy that rejects `sameShape` matches resolved
 *                                     only by lineDelta or fallback.
 */
class ExtendedFingerprintIntegrationTest {

    private val includeAbsent = BaselineCalculation.Options(true)

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
        BaselineCalculation.compare(report, baseline, includeAbsent)

    private fun Result.matchedBy(): String? = properties?.get("matchedBy") as? String

    @Test
    fun `equalIndicator matches when fingerprints are identical`() {
        val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp")))
        val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "fp")))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v2", r.matchedBy())
    }

    @Test
    fun `equalIndicator wins over later phases when both could match`() {
        val r = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "fp"),
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc"),
            ),
        )
        val b = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "fp"),
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v2", r.matchedBy())
    }

    @Test
    fun `resultKey matches identical content when there are no fingerprints`() {
        val r = result(message = "same", filePath = "src/a.kt")
        val b = result(message = "same", filePath = "src/a.kt")

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("resultKey", r.matchedBy())
    }

    @Test
    fun `resultKey wins over hash cascade for content-stable results`() {
        // Both phases (resultKey and the structural cascade) would match this pair,
        // but resultKey runs first and takes the win.
        val r = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc")),
        )
        val b = result(
            message = "same", filePath = "src/a.kt",
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc")),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("resultKey", r.matchedBy())
    }

    @Test
    fun `resultKey collisions are resolved by the tiebreaker chain`() {
        // Same ResultKey on multiple report candidates (same message, URI, snippet, charLength)
        // disambiguated by contextSnippet.
        val ctx = "before\nproblem\nafter"
        val b = result(filePath = "src/a.kt", contextSnippet = ctx)
        val r1 = result(filePath = "src/a.kt", contextSnippet = ctx)
        val r2 = result(filePath = "src/a.kt", contextSnippet = "different\n")

        val calc = compare(report(r1, r2), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals("resultKey+contextSnippet", r1.matchedBy())
    }

    @Test
    fun `cascade catches matches when content changed but structure preserved`() {
        // Different message AND different file: resultKey cannot match.
        // sameLocationAndShape hashes agree → cascade rescues.
        val r = result(
            message = "refactored", filePath = "src/new.kt",
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc")),
        )
        val b = result(
            message = "original", filePath = "src/old.kt",
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc")),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("sameLocationAndShape/v1", r.matchedBy())
    }

    @Test
    fun `cascade falls through to sameFuncAndShape when top tier hash differs`() {
        val r = result(
            message = "refactored", filePath = "src/new.kt",
            fingerprints = mapOf(
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "diffA"),
                SAME_FUNC_AND_SHAPE to mapOf(1 to "match"),
            ),
        )
        val b = result(
            message = "original", filePath = "src/old.kt",
            fingerprints = mapOf(
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "diffB"),
                SAME_FUNC_AND_SHAPE to mapOf(1 to "match"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("sameFuncAndShape/v1", r.matchedBy())
    }

    @Test
    fun `cascade falls through to sameShape when both higher tiers differ`() {
        val r = result(
            message = "refactored", filePath = "src/new.kt",
            fingerprints = mapOf(
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "diffA"),
                SAME_FUNC_AND_SHAPE to mapOf(1 to "diffA"),
                SAME_SHAPE to mapOf(1 to "shape"),
            ),
        )
        val b = result(
            message = "original", filePath = "src/old.kt",
            fingerprints = mapOf(
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "diffB"),
                SAME_FUNC_AND_SHAPE to mapOf(1 to "diffB"),
                SAME_SHAPE to mapOf(1 to "shape"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("sameShape/v1", r.matchedBy())
    }

    @Test
    fun `sameShape match is REJECTED when only lineDelta could resolve the tie`() {
        // Multiple report candidates share the same sameShape hash, no content-based filter
        // can distinguish them, and the only discriminator is line proximity.
        // Policy rejects this match → result is NEW, baseline is ABSENT.
        val b = result(
            message = "bm", filePath = "src/b.kt", startLine = 10,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r1 = result(
            message = "r1", filePath = "src/r1.kt", startLine = 12,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r2 = result(
            message = "r2", filePath = "src/r2.kt", startLine = 200,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(2, calc.newResults)
        assertEquals(1, calc.absentResults)
    }

    @Test
    fun `sameShape match is REJECTED when fallback is needed`() {
        // Two candidates with same sameShape hash and no distinguishing signal at all
        // → cascade reaches fallback. Policy rejects.
        val b = result(
            message = "bm", filePath = "src/b.kt",
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r1 = result(
            message = "r1", filePath = "src/r1.kt",
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r2 = result(
            message = "r2", filePath = "src/r2.kt",
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(2, calc.newResults)
    }

    @Test
    fun `sameShape match is ACCEPTED when contextSnippet resolves the tie`() {
        val ctx = "before\nproblem\nafter"
        val b = result(
            message = "bm", filePath = "src/b.kt", contextSnippet = ctx,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r1 = result(
            message = "r1", filePath = "src/r1.kt", contextSnippet = ctx,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r2 = result(
            message = "r2", filePath = "src/r2.kt", contextSnippet = "different\n",
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals("sameShape/v1+contextSnippet", r1.matchedBy())
    }

    @Test
    fun `sameShape match is ACCEPTED when columnDelta resolves the tie`() {
        // columnDelta is NOT in the rejection set — only lineDelta and fallback are.
        val b = result(
            filePath = "src/b.kt", startLine = 10, startColumn = 5,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r1 = result(
            filePath = "src/r1.kt", startLine = 10, startColumn = 5,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )
        val r2 = result(
            filePath = "src/r2.kt", startLine = 10, startColumn = 80,
            fingerprints = mapOf(SAME_SHAPE to mapOf(1 to "h")),
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("sameShape/v1+columnDelta", r1.matchedBy())
    }

    @Test
    fun `policy does NOT apply to sameLocationAndShape with lineDelta tiebreaker`() {
        // Same scenario as the rejected one, but on sameLocationAndShape (strongest hash).
        // Policy targets only sameShape — this match is committed.
        val b = result(
            message = "bm", filePath = "src/b.kt", startLine = 10,
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "h")),
        )
        val r1 = result(
            message = "r1", filePath = "src/r1.kt", startLine = 12,
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "h")),
        )
        val r2 = result(
            message = "r2", filePath = "src/r2.kt", startLine = 200,
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "h")),
        )

        val calc = compare(report(r1, r2), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals("sameLocationAndShape/v1+lineDelta", r1.matchedBy())
    }

    @Test
    fun `matches at the greatest common fingerprint version`() {
        val r = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "v1", 2 to "v2")))
        val b = result(fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "v1", 2 to "v2")))

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v2", r.matchedBy())
    }

    @Test
    fun `no fallback to lower version when higher common version disagrees`() {
        // Both sides have v1 and v2. v2 is the highest common version; the v2 hashes differ,
        // so there is no match (we do not silently fall back to v1).
        val r = result(
            filePath = "src/r.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "shared", 2 to "r")),
        )
        val b = result(
            filePath = "src/b.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "shared", 2 to "b")),
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
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "shared", 2 to "r_only")),
        )
        val b = result(
            filePath = "src/b.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(1 to "shared")),
        )

        val calc = compare(report(r), report(b))

        assertEquals(1, calc.unchangedResults)
        assertEquals("equalIndicator/v1", r.matchedBy())
    }

    @Test
    fun `stronger phase wins globally regardless of baseline iteration order`() {
        // R1 has BOTH equalIndicator (matches B2) AND sameLocationAndShape (matches B1).
        // Only R1 exists, so it can go to one baseline.
        //
        // Under batch-per-phase, Phase 1 (equalIndicator) runs across ALL baselines first.
        // B2 takes R1 via equalIndicator before the cascade phase ever runs for B1.
        // B1 then has nothing left to match.
        val b1 = result(
            message = "b1-cascade", filePath = "src/b1.kt",
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc")),
        )
        val b2 = result(
            message = "b2-eq", filePath = "src/b2.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "eq")),
        )
        val r1 = result(
            message = "r1-both", filePath = "src/r1.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "eq"),
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc"),
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
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "r_loc"),
            ),
        )
        val b = result(
            message = "b-msg", filePath = "src/b.kt",
            fingerprints = mapOf(
                EQUAL_INDICATOR to mapOf(2 to "b_eq"),
                SAME_LOCATION_AND_SHAPE to mapOf(1 to "b_loc"),
            ),
        )

        val calc = compare(report(r), report(b))

        assertEquals(0, calc.unchangedResults)
        assertEquals(1, calc.newResults)
        assertEquals(1, calc.absentResults)
    }

    @Test
    fun `each result is matched by the strongest available phase`() {
        // 1) matched by equalIndicator
        val r1 = result(message = "m1", filePath = "src/eq.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "eq1")))
        val b1 = result(message = "m1", filePath = "src/eq.kt",
            fingerprints = mapOf(EQUAL_INDICATOR to mapOf(2 to "eq1")))

        // 2) matched by resultKey (content-stable, no fingerprints)
        val r2 = result(message = "m2", filePath = "src/rk.kt")
        val b2 = result(message = "m2", filePath = "src/rk.kt")

        // 3) matched by sameLocationAndShape (cascade only — content has changed)
        val r3 = result(message = "m3-new", filePath = "src/cs.kt",
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc")))
        val b3 = result(message = "m3-old", filePath = "src/cs.kt",
            fingerprints = mapOf(SAME_LOCATION_AND_SHAPE to mapOf(1 to "loc")))

        val calc = compare(report(r1, r2, r3), report(b1, b2, b3))

        assertEquals(3, calc.unchangedResults)
        assertEquals(0, calc.newResults)
        assertEquals(0, calc.absentResults)
        assertEquals("equalIndicator/v2", r1.matchedBy())
        assertEquals("resultKey", r2.matchedBy())
        assertEquals("sameLocationAndShape/v1", r3.matchedBy())
    }
}
