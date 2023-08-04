package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class SerializeTest {
    @Test
    public void testPropertyBag() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/properties.json");
        String targetJson = Files.readString(target);
        PropertyBag bag = new PropertyBag();
        bag.put("someProp", "someValue");
        bag.put("someArray", Arrays.asList("1", "2", "3"));
        bag.getTags().add("someTag");
        SarifReport report = new SarifReport().withProperties(bag);
        doTest(targetJson, report);
    }

    @Test
    public void testPartialFingerprint() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/partialFingerprint.json");
        String targetJson = Files.readString(target);
        VersionedMap<String> partialFingerprints = new VersionedMap<>();
        partialFingerprints.put("idea", 1, "value");
        SarifReport report = new SarifReport()
                .withRuns(List.of(new Run().withResults(List.of(
                        new Result().withPartialFingerprints(partialFingerprints)))
                ));
        doTest(targetJson, report);
    }

    private void doTest(String targetJson, SarifReport report) throws IOException {
        StringWriter writer = new StringWriter();
        SarifUtil.writeReport(writer, report);
        writer.close();
        String writeResult = writer.toString();
        Assert.assertEquals(targetJson, writeResult);
    }

}
