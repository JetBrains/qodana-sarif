package com.jetbrains.qodana.sarif;

import com.google.gson.Gson;
import com.jetbrains.qodana.sarif.model.PropertyBag;
import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.SarifReport;
import com.jetbrains.qodana.sarif.model.streaming.IndexedResult;
import com.jetbrains.qodana.sarif.model.streaming.ResultLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StreamParseCorrectnessTest extends StreamParseTest {
    private static final Gson gson = new Gson();
    @Test
    public void testStreamParseMakeSameAsSimpleParseReadingResultsOneByOne() {
        forEachInput(inputPath -> {
            SarifReport expectedReport = read(inputPath, false);
            SarifReport actualReport = read(inputPath, true);

            assertEqualContents(
                expectedReport,
                new StreamingSarifReport(actualReport, () -> makeReader(inputPath)),
                (expected, actual, index) -> {
                    Iterator<Result> expectedResults = expected.getResults().iterator();
                    actual.getReader(reader -> {
                        Iterator<Result> actualResults = SarifUtil.lazyReadResults(reader, index);
                        while (expectedResults.hasNext()) {
                            assertTrue(actualResults.hasNext());
                            assertEquals(expectedResults.next(), actualResults.next());
                        }
                        assertEquals(expected.withResults(null).withProperties(null), actual.getRun().withResults(null).withProperties(null));
                        assertFalse(actualResults.hasNext());

                    });
                    assertRunPropertyBag(expected, actual, index);
                }
            );
        });
    }

    @Test
    public void testStreamParseMakeSameAsSimpleParseReadingResultsTogether() {
        forEachInput(inputPath -> {
            SarifReport expectedReport = read(inputPath, false);
            SarifReport actualReport = read(inputPath, true);

            boolean[] anyLeftAtLastRun = { true };

            assertEqualContents(
                expectedReport,
                new StreamingSarifReport(actualReport, () -> makeReader(inputPath)),
                (expected, actual, index) -> {
                    actual.getReader(reader -> {
                        Iterator<Result> expectedResults = expected.getResults().iterator();
                        Iterator<IndexedResult> actualResults = SarifUtil.lazyReadIndexedResults(reader);
                        while (expectedResults.hasNext()) {
                            IndexedResult indexedResult;
                            do {
                                assertTrue(actualResults.hasNext());
                                indexedResult = actualResults.next();
                            } while (indexedResult.getIndex() != index);
                            assertEquals((int) index, indexedResult.getIndex());
                            assertEquals(expectedResults.next(), indexedResult.getResult());
                            anyLeftAtLastRun[0] = actualResults.hasNext();
                        }
                        assertEquals(expected.withResults(null).withProperties(null), actual.getRun().withResults(null).withProperties(null));
                    });
                    assertRunPropertyBag(expected, actual, index);
                }
            );
            assertFalse(anyLeftAtLastRun[0]);
        });
    }

    @Test
    public void testStreamParseReadsSanityCorrectly() throws IOException {
        SarifReport expectedReport = read(sanityPath, false);
        String propertyName = "qodana.sanity.results";

        try (Reader reader = makeReader(sanityPath)) {
            Iterator<Result> expectedResults = SarifUtil
                    .readResultsFromObject(expectedReport.getRuns().get(0).getProperties().get(propertyName))
                    .iterator();
            Iterator<Result> actualResults = SarifUtil.lazyReadResultsFromLocation(
                    reader,
                    new ResultLocation.InProperties(0, propertyName)
            );
            while (expectedResults.hasNext()) {
                assertTrue(actualResults.hasNext());
                assertEquals(expectedResults.next(), actualResults.next());
            }
            assertFalse(actualResults.hasNext());
        }
    }

    private static void assertRunPropertyBag(Run expected, StreamingRun actual, Integer runIndex) throws IOException {
        if (expected.getProperties() != null) {
            actual.getReader(reader -> {
                Iterator<Object> expectedSanityResults = ((List<Object>) expected.getProperties().getOrDefault(
                        "qodana.sanity.results",
                        Collections.emptyList()
                )).iterator();
                Iterator<Result> actualSanityResults = SarifUtil.lazyReadResultsFromLocation(
                        reader,
                        new ResultLocation.InProperties(runIndex, "qodana.sanity.results")
                );
                while (expectedSanityResults.hasNext()) {
                    assertTrue(actualSanityResults.hasNext());
                    Result expectedSanityResult = gson.fromJson(gson.toJson(expectedSanityResults.next()), Result.class);
                    assertEquals(expectedSanityResult, actualSanityResults.next());
                }
                PropertyBag expectedProperties = expected.getProperties();
                expectedProperties.remove("qodana.sanity.results");
                assertEquals(expectedProperties, actual.getRun().getProperties());
            });
        }
    }
}
