package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.PropertyBag;
import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.SarifReport;
import com.jetbrains.qodana.sarif.model.VersionedMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeserializeTest {
    @Test
    public void testPropertyBag() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/properties.json");
        PropertyBag bag = new PropertyBag();
        bag.put("someProp", "someValue");
        bag.put("someArray", Arrays.asList("1", "2", "3"));
        bag.getTags().add("someTag");
        SarifReport report = new SarifReport().withProperties(bag);
        doTest(target, report);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPropertyBagThrows() throws IOException {
        new PropertyBag().put("tags", Arrays.asList("1", "2", "3"));
    }

    @Test
    public void testFingerprints() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/partialFingerprint.json");
        VersionedMap<String> fingerprints = new VersionedMap<>();
        fingerprints.put("idea", 1, "value");

        assertEquals(fingerprints,
                SarifUtil.readReport(target).getRuns().get(0).getResults().iterator().next().getPartialFingerprints());
    }

    @Test
    public void testPromoAndSanity() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/real.sarif.json");

        PropertyBag properties = SarifUtil.readReport(target).getRuns().get(0).getProperties();
        List<Result> promo = SarifUtil.readResultsFromObject(properties.get("qodana.promo.results"));
        List<Result> sanity = SarifUtil.readResultsFromObject(properties.get("qodana.sanity.results"));

        Assert.assertEquals(7, promo.size());
        Assert.assertEquals(2, sanity.size());
    }


    private void doTest(Path targetJsonPath, SarifReport expected) throws IOException {
        SarifReport sarifReport = SarifUtil.readReport(targetJsonPath);
        assertEquals(expected, sarifReport);
    }
}
