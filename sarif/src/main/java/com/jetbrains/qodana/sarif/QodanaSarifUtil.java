package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.SarifReport;
import com.jetbrains.qodana.sarif.model.streaming.ResultLocation;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QodanaSarifUtil {
    private QodanaSarifUtil() {
    }

    public static SarifReport readReport(Reader reader, boolean readResults, boolean readPromo, boolean readSanity) {
        List<String> skippedProperties = new ArrayList<>();

        if (!readPromo) {
            skippedProperties.add(PROMO_PROPERTY_NAME);
        }

        if (!readSanity) {
            skippedProperties.add(SANITY_PROPERTY_NAME);

        }
        return SarifUtil.readReport(reader, readResults, skippedProperties);
    }

    public static Iterator<Result> lazyReadResults(Reader reader) {
        return lazyReadResults(reader, 0);
    }

    public static Iterator<Result> lazyReadSanity(Reader reader) {
        return lazyReadSanity(reader, 0);
    }

    public static Iterator<Result> lazyReadPromo(Reader reader) {
        return lazyReadPromo(reader, 0);
    }

    public static Iterator<Result> lazyReadResults(Reader reader, int runIndex) {
        ResultLocation location = new ResultLocation.InRun(runIndex);
        return SarifUtil.lazyReadResultsFromLocation(reader, location);
    }

    public static Iterator<Result> lazyReadSanity(Reader reader, int runIndex) {
        ResultLocation location = new ResultLocation.InProperties(runIndex, SANITY_PROPERTY_NAME);
        return SarifUtil.lazyReadResultsFromLocation(reader, location);
    }

    public static Iterator<Result> lazyReadPromo(Reader reader, int runIndex) {
        ResultLocation location = new ResultLocation.InProperties(runIndex, PROMO_PROPERTY_NAME);
        return SarifUtil.lazyReadResultsFromLocation(reader, location);
    }

    public static final String PROMO_PROPERTY_NAME = "qodana.promo.results";
    public static final String SANITY_PROPERTY_NAME = "qodana.sanity.results";
}
