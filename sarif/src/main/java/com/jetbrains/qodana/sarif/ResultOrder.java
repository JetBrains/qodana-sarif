package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation;
import com.jetbrains.qodana.sarif.model.ArtifactLocation;
import com.jetbrains.qodana.sarif.model.Location;
import com.jetbrains.qodana.sarif.model.PhysicalLocation;
import com.jetbrains.qodana.sarif.model.Region;
import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.VersionedMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts a {@code results} array by {@code uri}, {@code charOffset}, {@code ruleId} and {@code equalIndicator/v1},
 * so that the same code always gives the same report and adding one problem adds one block to the diff instead of
 * shuffling the whole file.
 */
public final class ResultOrder {
    private static final Comparator<String> TEXT = Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<Integer> NUMBER = Comparator.nullsFirst(Comparator.naturalOrder());

    /** Content-derived order over results. Tolerates {@code null} results, which sort last */
    public static final Comparator<Result> CANONICAL = Comparator.nullsLast(
            Comparator.comparing(ResultOrder::uri, TEXT)
                    .thenComparing(ResultOrder::charOffset, NUMBER)
                    .thenComparing(Result::getRuleId, TEXT)
                    .thenComparing(ResultOrder::equalIndicator, TEXT));

    private ResultOrder() {
    }

    /** @return a new list holding {@code results} in {@link #CANONICAL} order */
    public static List<Result> sorted(Collection<Result> results) {
        if (results == null) return null;
        List<Result> sorted = new ArrayList<>(results);
        sorted.sort(CANONICAL);
        return sorted;
    }

    private static PhysicalLocation primaryLocation(Result result) {
        List<Location> locations = result.getLocations();
        Location primary = locations == null || locations.isEmpty() ? null : locations.get(0);
        return primary == null ? null : primary.getPhysicalLocation();
    }

    private static String uri(Result result) {
        PhysicalLocation location = primaryLocation(result);
        ArtifactLocation artifact = location == null ? null : location.getArtifactLocation();
        return artifact == null ? null : artifact.getUri();
    }

    private static Integer charOffset(Result result) {
        PhysicalLocation location = primaryLocation(result);
        Region region = location == null ? null : location.getRegion();
        return region == null ? null : region.getCharOffset();
    }

    private static String equalIndicator(Result result) {
        VersionedMap<String> fingerprints = result.getPartialFingerprints();
        return fingerprints == null ? null : fingerprints.get(BaselineCalculation.EQUAL_INDICATOR, 1);
    }
}
