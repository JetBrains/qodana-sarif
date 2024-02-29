package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


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

    @Test
    public void testPropertyBagThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PropertyBag().put("tags", Arrays.asList("1", "2", "3"))
        );
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

        assertEquals(7, promo.size());
        assertEquals(2, sanity.size());
    }

    @Test
    public void testDateFormat() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/isoDates.json");
        List<Invocation> invocations = SarifUtil.readReport(target).getRuns().get(0).getInvocations();

        OffsetDateTime expect = LocalDate.of(2016, 2, 8).atStartOfDay().atOffset(ZoneOffset.UTC);
        assertEquals(expect.toInstant(), invocations.get(0).getStartTimeUtc());

        expect = expect.plusHours(16).plusMinutes(8);
        assertEquals(expect.toInstant(), invocations.get(0).getEndTimeUtc());

        expect = expect.plusSeconds(25);
        assertEquals(expect.toInstant(), invocations.get(1).getStartTimeUtc());

        expect = expect.plus(943, ChronoUnit.MILLIS);
        assertEquals(expect.toInstant(), invocations.get(1).getEndTimeUtc());
    }

    @Test
    public void testIgnoreProperties() throws IOException {
        Path target = Paths.get("src/test/resources/testData/serializeTest/real.sarif.json");
        try (Reader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            SarifReport report = SarifUtil.readReport(reader, false, Collections.singletonList("qodana.sanity.results"));
            Assertions.assertFalse(report.getRuns().get(0).getProperties().containsKey("qodana.sanity.results"));
            Assertions.assertTrue(report.getRuns().get(0).getProperties().containsKey("qodana.promo.results"));
            Assertions.assertTrue(report.getRuns().get(0).getProperties().containsKey("deviceId"));
        }
    }


    private void doTest(Path targetJsonPath, SarifReport expected) throws IOException {
        SarifReport sarifReport = SarifUtil.readReport(targetJsonPath);
        assertEquals(expected, sarifReport);
    }
}
