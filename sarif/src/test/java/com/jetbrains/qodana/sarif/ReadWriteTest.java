package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.SarifReport;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReadWriteTest {
    @Test
    public void testComprehensiveReport() throws IOException {
        Path input = Paths.get("src/test/resources/testData/readWriteTest/qodanaReport.json");
        String reportJson = Files.readString(input);
        StringReader stringReader = new StringReader(reportJson);
        SarifReport report = SarifUtil.readReport(stringReader);
        StringWriter writer = new StringWriter();
        SarifUtil.writeReport(writer, report);
        writer.close();
        String writeResult = writer.toString();
        Assert.assertEquals(reportJson, writeResult);
    }

}
