package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.SarifReport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ReadWriteTest {
    @Test
    public void testComprehensiveReport() throws IOException {
        String reportJson = TestUtils.readStringFromPath("src/test/resources/testData/readWriteTest/qodanaReport.json");
        StringReader stringReader = new StringReader(reportJson);
        SarifReport report = SarifUtil.readReport(stringReader);
        StringWriter writer = new StringWriter();
        SarifUtil.writeReport(writer, report);
        writer.close();
        String writeResult = writer.toString();
        assertEquals(reportJson, writeResult);
    }

}
