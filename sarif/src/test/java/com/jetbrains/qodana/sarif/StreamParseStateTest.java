package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.SarifReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StreamParseStateTest extends StreamParseTest {
    @Test
    public void testListOfResultsIsNullIfStreamParsing() {
        forEachInput(inputPath -> {
            for (Run run : read(inputPath, true).getRuns()) {
                assertNull(run.getResults());
            }
        });
    }

    @Test
    public void testCannotUseListsWhenStreaming() {
        forEachInput(inputPath -> {
            SarifReport report = read(inputPath, true);
            for (Run run : report.getRuns()) {
                assertNull(run.getResults());
            }
        });
    }

    @Test
    public void testCanUseListsWhenNotStreaming() {
        forEachInput(inputPath -> {
            SarifReport report = read(inputPath, false);
            for (Run run : report.getRuns()) {
                assertNotNull(run.getResults());
            }
        });
    }
}
