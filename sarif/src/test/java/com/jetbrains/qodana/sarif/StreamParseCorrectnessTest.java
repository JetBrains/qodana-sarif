package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.SarifReport;
import com.jetbrains.qodana.sarif.model.streaming.IndexedResult;
import com.jetbrains.qodana.sarif.model.streaming.ResultLocation;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

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
                        Assert.assertTrue(actualResults.hasNext());
                        Assert.assertEquals(expectedResults.next(), actualResults.next());
                    }
                    Assert.assertEquals(expected.withResults(null), actual.getRun().withResults(null));
                    Assert.assertFalse(actualResults.hasNext());
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
                            Assert.assertTrue(actualResults.hasNext());
                            indexedResult = actualResults.next();
                        } while (indexedResult.getIndex() != index);
                        Assert.assertEquals((int)index, indexedResult.getIndex());
                        Assert.assertEquals(expectedResults.next(), indexedResult.getResult());
                        anyLeftAtLastRun[0] = actualResults.hasNext();
                    }
                    Assert.assertEquals(expected.withResults(null), actual.getRun().withResults(null));
                }
            );
            Assert.assertFalse(anyLeftAtLastRun[0]);
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
                Assert.assertTrue(actualResults.hasNext());
                Assert.assertEquals(expectedResults.next(), actualResults.next());
            }
            Assert.assertFalse(actualResults.hasNext());
        }
    }
}
