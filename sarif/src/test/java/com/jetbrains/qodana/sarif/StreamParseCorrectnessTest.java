package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.SarifReport;
import com.jetbrains.qodana.sarif.model.streaming.IndexedResult;
import com.jetbrains.qodana.sarif.model.streaming.ResultLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class StreamParseCorrectnessTest extends StreamParseTest {
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
                    Iterator<Result> actualResults = SarifUtil.lazyReadResults(actual.getReader(), index);
                    while (expectedResults.hasNext()) {
                        assertTrue(actualResults.hasNext());
                        assertEquals(expectedResults.next(), actualResults.next());
                    }
                    assertEquals(expected.withResults(null), actual.getRun().withResults(null));
                    assertFalse(actualResults.hasNext());
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
                    Iterator<Result> expectedResults = expected.getResults().iterator();
                    Iterator<IndexedResult> actualResults = SarifUtil.lazyReadIndexedResults(actual.getReader());
                    while (expectedResults.hasNext()) {
                        IndexedResult indexedResult;
                        do {
                            assertTrue(actualResults.hasNext());
                            indexedResult = actualResults.next();
                        } while (indexedResult.getIndex() != index);
                        assertEquals((int)index, indexedResult.getIndex());
                        assertEquals(expectedResults.next(), indexedResult.getResult());
                        anyLeftAtLastRun[0] = actualResults.hasNext();
                    }
                    assertEquals(expected.withResults(null), actual.getRun().withResults(null));
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
}
