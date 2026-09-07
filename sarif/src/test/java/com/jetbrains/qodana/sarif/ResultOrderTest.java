package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation;
import com.jetbrains.qodana.sarif.model.ArtifactLocation;
import com.jetbrains.qodana.sarif.model.ExternalProperties;
import com.jetbrains.qodana.sarif.model.Location;
import com.jetbrains.qodana.sarif.model.Message;
import com.jetbrains.qodana.sarif.model.PhysicalLocation;
import com.jetbrains.qodana.sarif.model.PropertyBag;
import com.jetbrains.qodana.sarif.model.Region;
import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.SarifReport;
import com.jetbrains.qodana.sarif.model.Tool;
import com.jetbrains.qodana.sarif.model.ToolComponent;
import com.jetbrains.qodana.sarif.model.VersionedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The report is written in a content-derived order, so that a diff of two consecutive reports shows the problems that
 * actually changed and nothing else.
 * <p>
 * The order key is {@code uri}, {@code charOffset}, {@code ruleId}, {@code equalIndicator/v1}.
 * Every result below is labeled through its message, labels play no part in the order, so asserting on them shows
 * which result landed where without the assertion depending on the thing under test.
 */
public class ResultOrderTest {
    private static final String QODANA_REPORT_JSON = "src/test/resources/testData/readWriteTest/qodanaReport.json";

    // ------------------------------------------------------------------------------------------------------
    // Each key, in isolation, and the precedence between them.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class KeyPrecedence {
        @Test
        void uriDecides() {
            assertOrder(Arrays.asList("a", "b", "c"),
                    labeled("c", "c.java", 1, 1),
                    labeled("a", "a.java", 1, 1),
                    labeled("b", "b.java", 1, 1));
        }

        /** The offset runs from the start of the file, so it orders a file's problems across lines as well. */
        @Test
        void charOffsetDecidesWithinAFile() {
            assertOrder(Arrays.asList("l2", "l10", "l100"),
                    labeled("l100", "a.java", 100, 3000),
                    labeled("l2", "a.java", 2, 40),
                    labeled("l10", "a.java", 10, 300));
        }

        @Test
        void charOffsetIsComparedNumericallyNotAsText() {
            // "1000" < "999" as text; 999 < 1000 as numbers. The latter is what the order must use.
            assertOrder(Arrays.asList("nineNineNine", "thousand"),
                    labeled("thousand", "a.java", 40, 1000),
                    labeled("nineNineNine", "a.java", 39, 999));
        }

        @Test
        void severalProblemsOnOneLineAreOrderedByOffset() {
            assertOrder(Arrays.asList("first", "second", "third"),
                    labeled("third", "a.java", 7, 300),
                    labeled("first", "a.java", 7, 100),
                    labeled("second", "a.java", 7, 200));
        }

        @Test
        void ruleIdDecidesAtOneOffset() {
            assertOrder(Arrays.asList("aRule", "mRule", "zRule"),
                    labeled("zRule", "a.java", 7, 100).withRuleId("ZRule"),
                    labeled("aRule", "a.java", 7, 100).withRuleId("ARule"),
                    labeled("mRule", "a.java", 7, 100).withRuleId("MRule"));
        }

        @Test
        void equalIndicatorDecidesForTheSameRuleAtOneOffset() {
            assertOrder(Arrays.asList("aaa", "bbb", "ccc"),
                    indicator(labeled("ccc", "a.java", 7, 100).withRuleId("R"), "ccc"),
                    indicator(labeled("aaa", "a.java", 7, 100).withRuleId("R"), "aaa"),
                    indicator(labeled("bbb", "a.java", 7, 100).withRuleId("R"), "bbb"));
        }

        @Test
        void uriBeatsEveryLaterKey() {
            assertOrder(Arrays.asList("earlyFile", "lateFile"),
                    indicator(labeled("lateFile", "z.java", 1, 1).withRuleId("AAA"), "aaa"),
                    indicator(labeled("earlyFile", "a.java", 9999, 9999).withRuleId("ZZZ"), "zzz"));
        }

        @Test
        void charOffsetBeatsRuleId() {
            assertOrder(Arrays.asList("lowOffset", "highOffset"),
                    labeled("highOffset", "a.java", 7, 200).withRuleId("AAA"),
                    labeled("lowOffset", "a.java", 7, 100).withRuleId("ZZZ"));
        }

        @Test
        void ruleIdBeatsEqualIndicator() {
            assertOrder(Arrays.asList("earlyRule", "lateRule"),
                    indicator(labeled("lateRule", "a.java", 7, 100).withRuleId("ZZZ"), "aaa"),
                    indicator(labeled("earlyRule", "a.java", 7, 100).withRuleId("AAA"), "zzz"));
        }

        @Test
        void keysNotInTheOrderDoNotAffectIt() {
            // message, level, kind, id and the v2 fingerprint are all ignored.
            Result a = labeled("a", "a.java", 1, 1).withRuleId("R").withGuid("zzzzzzzz");
            Result b = labeled("b", "a.java", 2, 2).withRuleId("R").withGuid("aaaaaaaa");
            a.setPartialFingerprints(fingerprints(BaselineCalculation.EQUAL_INDICATOR, 2, "zzz"));
            b.setPartialFingerprints(fingerprints(BaselineCalculation.EQUAL_INDICATOR, 2, "aaa"));

            assertOrder(Arrays.asList("a", "b"), b, a);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Absent values: every level of the location chain, and every key, may be missing.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class MissingValues {
        @Test
        void resultWithoutLocationsSortsFirst() {
            assertOrder(Arrays.asList("noLocations", "located"),
                    labeled("located", "a.java", 1, 1),
                    bare("noLocations"));
        }

        @Test
        void everyBrokenLinkInTheLocationChainSortsFirst() {
            Result located = labeled("located", "a.java", 1, 1);
            Result nullLocationList = bare("nullLocationList").withLocations(null);
            Result emptyLocationList = bare("emptyLocationList").withLocations(new ArrayList<>());
            Result nullFirstLocation = bare("nullFirstLocation")
                    .withLocations(new ArrayList<>(Collections.singletonList(null)));
            Result noPhysicalLocation = bare("noPhysicalLocation")
                    .withLocations(Collections.singletonList(new Location()));
            Result noArtifactLocation = bare("noArtifactLocation")
                    .withLocations(Collections.singletonList(new Location().withPhysicalLocation(
                            new PhysicalLocation())));
            Result noUri = bare("noUri")
                    .withLocations(Collections.singletonList(new Location().withPhysicalLocation(
                            new PhysicalLocation().withArtifactLocation(new ArtifactLocation()))));

            List<Result> sorted = ResultOrder.sorted(Arrays.asList(located, nullLocationList, emptyLocationList,
                    nullFirstLocation, noPhysicalLocation, noArtifactLocation, noUri));

            assertEquals("located", labelOf(sorted.get(sorted.size() - 1)));
            assertEquals(6, sorted.indexOf(located));
        }

        @Test
        void resultWithoutARegionSortsBeforeOneWithARegion() {
            Result noRegion = bare("noRegion").withLocations(Collections.singletonList(
                    new Location().withPhysicalLocation(new PhysicalLocation()
                            .withArtifactLocation(new ArtifactLocation().withUri("a.java")))));

            assertOrder(Arrays.asList("noRegion", "offset1"), labeled("offset1", "a.java", 1, 1), noRegion);
        }

        @Test
        void missingCharOffsetSortsBeforeAnyCharOffset() {
            assertOrder(Arrays.asList("noOffset", "offset1"),
                    labeled("offset1", "a.java", 7, 1),
                    labeled("noOffset", "a.java", 7, null));
        }

        @Test
        void missingRuleIdSortsBeforeAnyRuleId() {
            assertOrder(Arrays.asList("noRule", "withRule"),
                    labeled("withRule", "a.java", 7, 100).withRuleId("AAA"),
                    labeled("noRule", "a.java", 7, 100));
        }

        @Test
        void missingEqualIndicatorSortsBeforeAnyIndicator() {
            Result noFingerprints = labeled("noFingerprints", "a.java", 7, 100).withRuleId("R");
            Result emptyFingerprints = labeled("emptyFingerprints", "a.java", 7, 100).withRuleId("R");
            emptyFingerprints.setPartialFingerprints(new VersionedMap<>());
            Result otherKey = labeled("otherKey", "a.java", 7, 100).withRuleId("R");
            otherKey.setPartialFingerprints(fingerprints("somethingElse", 1, "aaa"));
            Result withIndicator = indicator(labeled("withIndicator", "a.java", 7, 100).withRuleId("R"), "aaa");

            List<Result> sorted = ResultOrder.sorted(
                    Arrays.asList(withIndicator, noFingerprints, emptyFingerprints, otherKey));

            assertEquals("withIndicator", labelOf(sorted.get(3)));
        }

        /**
         * v2 is a different hash function, so falling back to it would compare unrelated values. Both results below
         * are arranged so that reading v2 instead of v1 would flip the order rather than merely leave it unchanged.
         */
        @Test
        void aResultCarryingOnlyVersionTwoHasNoIndicator() {
            Result v2Only = labeled("v2Only", "a.java", 7, 100).withRuleId("R");
            v2Only.setPartialFingerprints(fingerprints(BaselineCalculation.EQUAL_INDICATOR, 2, "zzz"));
            Result v1 = indicator(labeled("v1", "a.java", 7, 100).withRuleId("R"), "aaa");

            // Reading v2 would give "zzz" vs "aaa" and put v1 first; ignoring it leaves v2Only with no key at all.
            assertOrder(Arrays.asList("v2Only", "v1"), v1, v2Only);
        }

        @Test
        void versionOneIsUsedEvenWhenALaterVersionIsPresent() {
            Result both = labeled("both", "a.java", 7, 100).withRuleId("R");
            VersionedMap<String> fingerprints = fingerprints(BaselineCalculation.EQUAL_INDICATOR, 1, "zzz");
            fingerprints.put(BaselineCalculation.EQUAL_INDICATOR, 2, "aaa");
            both.setPartialFingerprints(fingerprints);
            Result v1Only = indicator(labeled("v1Only", "a.java", 7, 100).withRuleId("R"), "mmm");

            // By v1: "mmm" < "zzz". By the newest version: "aaa" < "mmm", which would reverse these.
            assertOrder(Arrays.asList("v1Only", "both"), both, v1Only);
        }

        @Test
        void nullResultsSortLast() {
            List<Result> sorted = ResultOrder.sorted(Arrays.asList(
                    null, labeled("b", "b.java", 1, 1), null, labeled("a", "a.java", 1, 1), null));

            assertEquals(Arrays.asList("a", "b", null, null, null),
                    sorted.stream().map(ResultOrderTest::labelOf).collect(Collectors.toList()));
        }

        @Test
        void aListOfOnlyNullsIsHandled() {
            assertEquals(3, ResultOrder.sorted(Arrays.asList(null, null, null)).size());
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // The point of the whole thing: the order depends on content and nothing else.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class Determinism {
        @Test
        void orderIsIndependentOfTheOrderResultsWereProducedIn() throws IOException {
            String expected = write(readReport());

            SarifReport shuffled = readReport();
            Collections.shuffle(results(shuffled), new Random(42));

            assertEquals(expected, write(shuffled));
        }

        @Test
        void everyShuffleOfARealReportSerializesIdentically() throws IOException {
            String expected = write(readReport());
            for (int seed = 0; seed < 20; seed++) {
                SarifReport shuffled = readReport();
                Collections.shuffle(results(shuffled), new Random(seed));
                assertEquals(expected, write(shuffled), "seed " + seed);
            }
        }

        @Test
        void reversingTheInputDoesNotChangeTheOutput() throws IOException {
            String expected = write(readReport());

            SarifReport reversed = readReport();
            Collections.reverse(results(reversed));

            assertEquals(expected, write(reversed));
        }

        @Test
        void orderIsIndependentOfHowTheBaselineMatched() throws IOException {
            SarifReport report = readReport();
            BaselineCalculation.compare(report, readReport());

            SarifReport shuffledReport = readReport();
            Collections.shuffle(results(shuffledReport), new Random(1));
            SarifReport shuffledBaseline = readReport();
            Collections.shuffle(results(shuffledBaseline), new Random(2));
            BaselineCalculation.compare(shuffledReport, shuffledBaseline);

            assertEquals(write(report), write(shuffledReport));
        }

        /**
         * With the report and the baseline differing, the matcher classifies results as NEW, UNCHANGED and ABSENT and
         * appends each class through a different path, so the array it produces is grouped by baseline state. Feeding
         * it the same difference twice, shuffled differently, must still serialize identically.
         */
        @Test
        void orderIsIndependentOfHowAMixedBaselineMatched() throws IOException {
            String expected = write(comparedAgainstBaseline(0));
            for (int seed = 1; seed <= 5; seed++) {
                assertEquals(expected, write(comparedAgainstBaseline(seed)), "seed " + seed);
            }
        }

        /**
         * Compares a report against a baseline it genuinely differs from: a third of the problems are dropped (they
         * become ABSENT) and two are added (they become NEW). {@code seed} only changes the order the two sides are
         * handed to the matcher in, never their content.
         */
        private SarifReport comparedAgainstBaseline(int seed) throws IOException {
            SarifReport report = readReport();
            List<Result> results = results(report);
            List<Result> kept = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                if (i % 3 != 0) kept.add(results.get(i));
            }
            // Unique indicators, as every real result has: the matcher pools candidates by equalIndicator, so two
            // results sharing one (here, sharing its absence) would collide and the survivor would depend on order.
            kept.add(indicator(labeled("addedOne", "aaa.java", 1, 1).withRuleId("AddedRule"), "addedOne"));
            kept.add(indicator(labeled("addedTwo", "zzz.java", 1, 1).withRuleId("AddedRule"), "addedTwo"));
            report.getRuns().get(0).setResults(kept);

            SarifReport baseline = readReport();
            if (seed > 0) {
                Collections.shuffle(kept, new Random(seed));
                Collections.shuffle(results(baseline), new Random(seed + 100));
            }
            BaselineCalculation.compare(report, baseline);
            return report;
        }

        @Test
        void sortingIsIdempotent() throws IOException {
            List<Result> once = ResultOrder.sorted(results(readReport()));
            List<Result> twice = ResultOrder.sorted(once);

            assertEquals(labels(once), labels(twice));
        }

        @Test
        void resultsIndistinguishableByEveryKeyKeepTheirIncomingOrder() {
            // No key can separate these, so the sort must be stable rather than arbitrary.
            Result first = labeled("first", "a.java", 7, 100).withRuleId("R");
            Result second = labeled("second", "a.java", 7, 100).withRuleId("R");
            Result third = labeled("third", "a.java", 7, 100).withRuleId("R");

            assertEquals(Arrays.asList("first", "second", "third"),
                    labels(ResultOrder.sorted(Arrays.asList(first, second, third))));
            assertEquals(Arrays.asList("third", "second", "first"),
                    labels(ResultOrder.sorted(Arrays.asList(third, second, first))));
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // ResultOrder.sorted contract.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class SortedContract {
        @Test
        void nullInputGivesNull() {
            assertNull(ResultOrder.sorted(null));
        }

        @Test
        void emptyInputGivesEmpty() {
            assertTrue(ResultOrder.sorted(new ArrayList<>()).isEmpty());
        }

        @Test
        void inputIsNeitherReorderedNorAliased() {
            Result a = labeled("a", "a.java", 1, 1);
            Result b = labeled("b", "b.java", 1, 1);
            List<Result> input = new ArrayList<>(Arrays.asList(b, a));

            List<Result> sorted = ResultOrder.sorted(input);

            assertNotSame(input, sorted);
            assertEquals(Arrays.asList("b", "a"), labels(input));
        }

        @Test
        void animmutableCollectionIsAccepted() {
            List<Result> singleton = Collections.singletonList(labeled("only", "a.java", 1, 1));
            assertEquals(Collections.singletonList("only"), labels(ResultOrder.sorted(singleton)));

            List<Result> fixedSize = Arrays.asList(labeled("b", "b.java", 1, 1), labeled("a", "a.java", 1, 1));
            assertEquals(Arrays.asList("a", "b"), labels(ResultOrder.sorted(fixedSize)));
        }

        @Test
        void anyCollectionIsAcceptedNotJustAList() {
            LinkedHashSet<Result> set = new LinkedHashSet<>(
                    Arrays.asList(labeled("b", "b.java", 1, 1), labeled("a", "a.java", 1, 1)));

            assertEquals(Arrays.asList("a", "b"), labels(ResultOrder.sorted(set)));
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Where the order gets applied: every place a results array can live, and nowhere else.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class Serialization {
        @Test
        void runResultsAreOrdered() throws IOException {
            Run run = emptyRun();
            run.setResults(new ArrayList<>(Arrays.asList(
                    labeled("z", "z.java", 1, 1), labeled("a", "a.java", 1, 1))));

            assertLabelOrder(write(reportOf(run)), "a", "z");
        }

        @Test
        void eachRunIsOrderedIndependently() throws IOException {
            Run first = emptyRun();
            first.setResults(new ArrayList<>(Arrays.asList(
                    labeled("run1z", "z.java", 1, 1), labeled("run1a", "a.java", 1, 1))));
            Run second = emptyRun();
            second.setResults(new ArrayList<>(Arrays.asList(
                    labeled("run2z", "z.java", 1, 1), labeled("run2a", "a.java", 1, 1))));

            assertLabelOrder(write(new SarifReport().withRuns(Arrays.asList(first, second))),
                    "run1a", "run1z", "run2a", "run2z");
        }

        @Test
        void inlineExternalPropertiesResultsAreOrdered() throws IOException {
            ExternalProperties external = new ExternalProperties();
            external.setResults(new ArrayList<>(Arrays.asList(
                    labeled("z", "z.java", 1, 1), labeled("a", "a.java", 1, 1))));
            SarifReport report = reportOf(emptyRun())
                    .withInlineExternalProperties(new LinkedHashSet<>(Collections.singletonList(external)));

            assertLabelOrder(write(report), "a", "z");
        }

        /** The linter stashes these in {@code run.properties}, where they are typed as plain {@link Object}. */
        @Test
        void propertyBagResultsAreOrdered() throws IOException {
            Run run = emptyRun();
            run.setProperties(new PropertyBag());
            Assertions.assertNotNull(run.getProperties());
            run.getProperties().put("qodana.sanity.results", new ArrayList<>(Arrays.asList(
                    labeled("sanityZ", "z.java", 1, 1), labeled("sanityA", "a.java", 1, 1))));
            run.getProperties().put("qodana.promo.results", new ArrayList<>(Arrays.asList(
                    labeled("promoZ", "z.java", 1, 1), labeled("promoA", "a.java", 1, 1))));

            String written = write(reportOf(run));

            assertTrue(written.indexOf("sanityA") < written.indexOf("sanityZ"), written);
            assertTrue(written.indexOf("promoA") < written.indexOf("promoZ"), written);
        }

        @Test
        void propertyBagListsThatAreNotResultsAreLeftAlone() throws IOException {
            Run run = emptyRun();
            run.setProperties(new PropertyBag());
            Assertions.assertNotNull(run.getProperties());
            run.getProperties().put("qodana.something", new ArrayList<>(Arrays.asList("zebra", "apple")));
            run.getProperties().getTags().addAll(Arrays.asList("zTag", "aTag"));

            String written = write(reportOf(run));

            assertTrue(written.indexOf("zebra") < written.indexOf("apple"), written);
            assertTrue(written.indexOf("zTag") < written.indexOf("aTag"), written);
        }

        @Test
        void nullResultsStayNullAndEmptyStaysEmpty() throws IOException {
            Run nullResults = emptyRun();
            assertFalse(write(reportOf(nullResults)).contains("\"results\""));

            Run emptyResults = emptyRun();
            emptyResults.setResults(new ArrayList<>());
            assertTrue(write(reportOf(emptyResults)).contains("\"results\": []"));
        }

        @Test
        void anImmutableResultsListCanBeWritten() throws IOException {
            Run run = emptyRun();
            run.setResults(Collections.singletonList(labeled("only", "a.java", 1, 1)));

            assertLabelOrder(write(reportOf(run)), "only");
        }

        @Test
        void aResultsListStartingWithNullIsOrderedAndKeepsItsNulls() throws IOException {
            Run run = emptyRun();
            run.setResults(new ArrayList<>(Arrays.asList(
                    null, labeled("z", "z.java", 1, 1), labeled("a", "a.java", 1, 1))));

            String written = write(reportOf(run));

            // Ordering the array must neither skip the leading null nor drop it from the output.
            assertLabelOrder(written, "a", "z");
            assertEquals(Arrays.asList("a", "z", null),
                    labels(results(SarifUtil.readReport(new StringReader(written), true))));
        }

        @Test
        void writingDoesNotReorderOrReplaceTheCallersList() throws IOException {
            SarifReport report = readReport();
            List<Result> results = results(report);
            List<Result> asProduced = new ArrayList<>(results);

            write(report);

            assertSame(results, results(report));
            assertEquals(asProduced, results);
        }

        @Test
        void readingDoesNotReorder() throws IOException {
            Run run = emptyRun();
            run.setResults(new ArrayList<>(Arrays.asList(
                    labeled("z", "z.java", 1, 1), labeled("a", "a.java", 1, 1))));
            // Written output is [a(a.java), z(z.java)]. Swapping the two uris leaves the file order untouched but
            // makes it contradict the canonical order, so a reader that sorted would hand back [z, a] instead.
            String outOfOrder = write(reportOf(run)).replace("a.java", "TMP").replace("z.java", "a.java")
                    .replace("TMP", "z.java");

            SarifReport reparsed = SarifUtil.readReport(new StringReader(outOfOrder), true);

            assertEquals(Arrays.asList("a", "z"), labels(results(reparsed)));
        }

        @Test
        void writeThenReadThenWriteIsStable() throws IOException {
            String once = write(readReport());
            String twice = write(SarifUtil.readReport(new StringReader(once), true));

            assertEquals(once, twice);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // The behaviour this exists for: a small change to the code is a small change to the report.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class DiffStability {
        @Test
        void addedProblemIsTheOnlyChangeInTheFile() throws IOException {
            SarifReport before = readReport();
            String writtenBefore = write(before);

            SarifReport after = readReport();
            List<Result> results = results(after);
            results.add(labeled("added", uriOfMedianProblem(results), 1, 1));
            Collections.shuffle(results, new Random(7));

            assertOneContiguousBlockDiffers(writtenBefore, write(after));
        }

        @Test
        void removedProblemIsTheOnlyChangeInTheFile() throws IOException {
            SarifReport before = readReport();
            List<Result> results = results(before);
            Result doomed = ResultOrder.sorted(results).get(results.size() / 2);
            String writtenBefore = write(before);

            SarifReport after = readReport();
            List<Result> remaining = results(after);
            remaining.removeIf(r -> r.equals(doomed));
            Collections.shuffle(remaining, new Random(11));

            assertOneContiguousBlockDiffers(write(after), writtenBefore);
        }

        /**
         * Inserting lines above a problem shifts its {@code startLine} and {@code charOffset} but must not move it
         * within the array — that is the property the location keys exist for.
         */
        @Test
        void shiftingAWholeFileKeepsEveryResultInItsSlot() throws IOException {
            SarifReport report = readReport();
            List<Result> before = ResultOrder.sorted(results(report));
            String busiest = uriOfMedianProblem(results(report));

            for (Result result : results(report)) {
                Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
                if (busiest.equals(result.getLocations().get(0).getPhysicalLocation()
                        .getArtifactLocation().getUri())) {
                    if (region.getStartLine() != null) region.setStartLine(region.getStartLine() + 5);
                    if (region.getCharOffset() != null) region.setCharOffset(region.getCharOffset() + 180);
                }
            }
            List<Result> after = ResultOrder.sorted(results(report));

            assertEquals(labels(before), labels(after), "the shifted file's problems changed places");
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Not every report has every key. Order by the ones that are there; leave the rest as they came.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class ReportsMissingOrderKeys {
        /** Guards the tests below from passing vacuously: this fixture really does contain indistinguishable results. */
        @Test
        void theFixtureReallyHasResultsNoKeyCanSeparate() throws IOException {
            List<Result> sorted = ResultOrder.sorted(readResults());
            int indistinguishable = 0;
            for (int i = 1; i < sorted.size(); i++) {
                if (ResultOrder.CANONICAL.compare(sorted.get(i - 1), sorted.get(i)) == 0) indistinguishable++;
            }
            assertTrue(indistinguishable > 0, "fixture no longer exercises the fallback");
            assertEquals(0, sorted.stream().filter(r -> r.getPartialFingerprints() != null).count());
        }

        @Test
        void whatCanBeOrderedIsOrdered() throws IOException {
            List<Result> input = readResults();
            Collections.shuffle(input, new Random(3));

            List<Result> sorted = ResultOrder.sorted(input);

            for (int i = 1; i < sorted.size(); i++) {
                assertTrue(ResultOrder.CANONICAL.compare(sorted.get(i - 1), sorted.get(i)) <= 0,
                        "result " + i + " is out of order");
            }
        }

        @Test
        void whatCannotBeOrderedKeepsTheOrderItCameIn() throws IOException {
            List<Result> input = readResults();
            Collections.shuffle(input, new Random(5));
            IdentityHashMap<Result, Integer> incoming = new IdentityHashMap<>();
            for (int i = 0; i < input.size(); i++) incoming.put(input.get(i), i);

            List<Result> sorted = ResultOrder.sorted(input);

            for (int i = 1; i < sorted.size(); i++) {
                Result previous = sorted.get(i - 1), current = sorted.get(i);
                if (ResultOrder.CANONICAL.compare(previous, current) == 0) {
                    assertTrue(incoming.get(previous) < incoming.get(current),
                            "two indistinguishable results were swapped at " + i);
                }
            }
        }

        @Test
        void noResultIsLostOrDuplicated() throws IOException {
            List<Result> input = readResults();

            List<Result> sorted = ResultOrder.sorted(input);

            assertEquals(input.size(), sorted.size());
            IdentityHashMap<Result, Boolean> seen = new IdentityHashMap<>();
            for (Result result : sorted) seen.put(result, Boolean.TRUE);
            assertEquals(input.size(), seen.size(), "a result was duplicated");
            for (Result result : input) assertTrue(seen.containsKey(result), "a result went missing");
        }

        @Test
        void writingIsStableEvenWhenNothingCanBeOrdered() throws IOException {
            String once = write(readReportUnstamped());
            String twice = write(SarifUtil.readReport(new StringReader(once), true));

            assertEquals(once, twice);
        }

        @Test
        void sortingIsIdempotentEvenWhenNothingCanBeOrdered() throws IOException {
            List<Result> input = readResults();
            Collections.shuffle(input, new Random(9));

            List<Result> once = ResultOrder.sorted(input);
            List<Result> twice = ResultOrder.sorted(once);

            assertEquals(labels(once), labels(twice));
        }

        private List<Result> readResults() throws IOException {
            return new ArrayList<>(results(readReportUnstamped()));
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // A report may carry nothing at all: no runs, no results, or no keys whatsoever.
    // ------------------------------------------------------------------------------------------------------

    @Nested
    class EmptyReports {
        @Test
        void reportWithoutRunsIsWrittenAndReadBack() throws IOException {
            String written = write(new SarifReport());

            assertEquals("{}", written);
            assertNull(SarifUtil.readReport(new StringReader(written), true).getRuns());
        }

        @Test
        void reportWithAnEmptyRunsListKeepsIt() throws IOException {
            String written = write(new SarifReport().withRuns(new ArrayList<>()));

            assertEquals("{\n  \"runs\": []\n}", written);
            assertTrue(SarifUtil.readReport(new StringReader(written), true).getRuns().isEmpty());
        }

        @Test
        void runWithoutResultsIsWrittenWithoutAResultsKey() throws IOException {
            String written = write(reportOf(emptyRun()));

            assertFalse(written.contains("\"results\""), written);
        }

        @Test
        void emptyReportHelperRoundTripsWithAnEmptyResultsArray() throws IOException {
            String written = write(SarifUtil.emptyReport("Qodana"));

            assertTrue(written.contains("\"results\": []"), written);
            assertTrue(results(SarifUtil.readReport(new StringReader(written), true)).isEmpty());
        }

        @Test
        void writingAnEmptyReportIsStable() throws IOException {
            String once = write(SarifUtil.emptyReport("Qodana"));
            String twice = write(SarifUtil.readReport(new StringReader(once), true));

            assertEquals(once, twice);
        }

        @Test
        void comparingTwoEmptyReportsAgainstEachOtherStillWrites() throws IOException {
            SarifReport report = SarifUtil.emptyReport("Qodana");
            BaselineCalculation.compare(report, SarifUtil.emptyReport("Qodana"));

            assertTrue(write(report).contains("\"results\": []"));
        }

        @Test
        void anEmptyRunAlongsideAPopulatedOneDoesNotDisturbIt() throws IOException {
            Run populated = emptyRun();
            populated.setResults(new ArrayList<>(Arrays.asList(
                    labeled("z", "z.java", 1, 900), labeled("a", "a.java", 1, 100))));

            String written = write(new SarifReport().withRuns(Arrays.asList(emptyRun(), populated)));

            assertLabelOrder(written, "a", "z");
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Helpers.
    // ------------------------------------------------------------------------------------------------------

    /** Asserts that {@code results} sort into exactly {@code expectedLabels}. */
    private static void assertOrder(List<String> expectedLabels, Result... results) {
        assertEquals(expectedLabels, labels(ResultOrder.sorted(Arrays.asList(results))));
    }

    /** Asserts that the labels appear in {@code written} in exactly this order. */
    private static void assertLabelOrder(String written, String... expectedLabels) {
        List<Integer> positions = Arrays.stream(expectedLabels)
                .map(label -> written.indexOf("\"" + label + "\""))
                .collect(Collectors.toList());
        assertFalse(positions.contains(-1), "a label is missing from the output: " + written);
        List<Integer> ascending = new ArrayList<>(positions);
        Collections.sort(ascending);
        assertEquals(ascending, positions, "labels are out of order in the output: " + written);
    }

    /** Every line of the smaller report survives in the larger one, with the difference as one block. */
    private static void assertOneContiguousBlockDiffers(String smaller, String larger) {
        String[] oldLines = smaller.split("\n", -1);
        String[] newLines = larger.split("\n", -1);
        assertTrue(newLines.length > oldLines.length, "nothing was added");

        int prefix = 0;
        while (prefix < oldLines.length && oldLines[prefix].equals(newLines[prefix])) prefix++;
        int suffix = 0;
        while (suffix < oldLines.length - prefix
                && oldLines[oldLines.length - 1 - suffix].equals(newLines[newLines.length - 1 - suffix])) suffix++;

        assertEquals(oldLines.length, prefix + suffix,
                "the report changed outside the added problem: " + (oldLines.length - prefix - suffix)
                        + " line(s) at " + (prefix + 1) + " were rewritten");
    }

    private static Result bare(String label) {
        return new Result(new Message().withText(label));
    }

    /** A result at a location, labeled through its message. A {@code null} line or offset is left out of the region. */
    private static Result labeled(String label, String uri, Integer startLine, Integer charOffset) {
        Region region = new Region();
        if (startLine != null) region.setStartLine(startLine);
        if (charOffset != null) region.setCharOffset(charOffset);
        Location location = new Location().withPhysicalLocation(new PhysicalLocation()
                .withArtifactLocation(new ArtifactLocation().withUri(uri))
                .withRegion(region));
        return bare(label).withLocations(new ArrayList<>(Collections.singletonList(location)));
    }

    private static Result indicator(Result result, String value) {
        result.setPartialFingerprints(fingerprints(BaselineCalculation.EQUAL_INDICATOR, 1, value));
        return result;
    }

    private static VersionedMap<String> fingerprints(String key, int version, String value) {
        VersionedMap<String> fingerprints = new VersionedMap<>();
        fingerprints.put(key, version, value);
        return fingerprints;
    }

    private static String labelOf(Result result) {
        return result == null ? null : result.getMessage().getText();
    }

    private static List<String> labels(Iterable<Result> results) {
        List<String> labels = new ArrayList<>();
        for (Result result : results) labels.add(labelOf(result));
        return labels;
    }

    private static Run emptyRun() {
        return new Run(new Tool(new ToolComponent("Qodana")));
    }

    private static SarifReport reportOf(Run run) {
        return new SarifReport().withRuns(new ArrayList<>(Collections.singletonList(run)));
    }

    private static String uriOfMedianProblem(List<Result> results) {
        List<Result> sorted = ResultOrder.sorted(results);
        Result median = sorted.get(sorted.size() / 2);
        return median.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();
    }

    private static List<Result> results(SarifReport report) {
        Run run = report.getRuns().get(0);
        if (!(run.getResults() instanceof ArrayList)) run.setResults(new ArrayList<>(run.getResults()));
        return run.getResults();
    }

    /**
     * The fixture predates fingerprints, so every result is stamped with one derived from its own content — the same
     * inputs a current analyzer hashes into {@code equalIndicator/v1}. Content-derived rather than positional, or the
     * stamp itself would decide the order and the determinism tests would prove nothing.
     * <p>
     * {@link ReportsMissingOrderKeys} reads the same file unstamped, to cover reports that carry no fingerprints.
     */
    private static SarifReport readReport() throws IOException {
        SarifReport report = readReportUnstamped();
        for (Result result : results(report)) {
            result.setPartialFingerprints(fingerprints(BaselineCalculation.EQUAL_INDICATOR, 1, contentHash(result)));
        }
        return report;
    }

    private static SarifReport readReportUnstamped() throws IOException {
        return SarifUtil.readReport(Paths.get(QODANA_REPORT_JSON));
    }

    private static String contentHash(Result result) {
        PhysicalLocation location = result.getLocations().get(0).getPhysicalLocation();
        Region region = location.getRegion();
        return "eq-" + Objects.hash(result.getRuleId(), location.getArtifactLocation().getUri(),
                region == null ? null : region.getStartLine(), region == null ? null : region.getCharOffset(),
                labelOf(result));
    }

    private static String write(SarifReport report) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            SarifUtil.writeReport(writer, report);
            return writer.toString();
        }
    }
}
