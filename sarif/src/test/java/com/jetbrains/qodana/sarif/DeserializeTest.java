package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.temporal.ChronoUnit;
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

    @Test
    public void testDateFormat() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/isoDates.json");
        List<Invocation> invocations = SarifUtil.readReport(target).getRuns().get(0).getInvocations();

        OffsetDateTime expect = LocalDate.of(2016, 2, 8).atStartOfDay().atOffset(ZoneOffset.UTC);
        Assert.assertEquals(expect.toInstant(), invocations.get(0).getStartTimeUtc());

        expect = expect.plusHours(16).plusMinutes(8);
        Assert.assertEquals(expect.toInstant(), invocations.get(0).getEndTimeUtc());

        expect = expect.plusSeconds(25);
        Assert.assertEquals(expect.toInstant(), invocations.get(1).getStartTimeUtc());

        expect = expect.plus(943, ChronoUnit.MILLIS);
        Assert.assertEquals(expect.toInstant(), invocations.get(1).getEndTimeUtc());
    }


    private void doTest(Path targetJsonPath, SarifReport expected) throws IOException {
        SarifReport sarifReport = SarifUtil.readReport(targetJsonPath);
        assertEquals(expected, sarifReport);
    }
}
