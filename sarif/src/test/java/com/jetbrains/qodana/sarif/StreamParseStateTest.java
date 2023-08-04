package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.SarifReport;
import org.junit.Assert;
import org.junit.Test;

public class StreamParseStateTest extends StreamParseTest {
    @Test
    public void testListOfResultsIsNullIfStreamParsing() {
        forEachInput(inputPath -> {
            for (Run run : read(inputPath, true).getRuns()) {
                Assert.assertNull(run.getResults());
            }
        });
    }

    @Test
    public void testCannotUseListsWhenStreaming() {
        forEachInput(inputPath -> {
            SarifReport report = read(inputPath, true);
            for (Run run : report.getRuns()) {
                Assert.assertNull(run.getResults());
            }
        });
    }

    @Test
    public void testCanUseListsWhenNotStreaming() {
        forEachInput(inputPath -> {
            SarifReport report = read(inputPath, false);
            for (Run run : report.getRuns()) {
                Assert.assertNotNull(run.getResults());
            }
        });
    }
}
