package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.*;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ResultKey {
    private final Result result;

    public ResultKey(Result r) {
        result = r;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (result == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultKey key = (ResultKey) o;
        Result oResult = key.result;

        if (!Objects.equals(result.getMessage(), oResult.getMessage()) ||
                !Objects.equals(result.getRuleId(), oResult.getRuleId()) ||
                !Objects.equals(result.getLevel(), oResult.getLevel())
        ) {
            return false;
        }

        List<Location> locations = result.getLocations();
        List<Location> oLocations = oResult.getLocations();
        if (locations == null) return oLocations == null;
        if (oLocations == null) return false;
        if (locations.size() != oLocations.size()) return false;

        Iterator<Location> iterator = locations.iterator();
        Iterator<Location> oIterator = oLocations.iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next();
            Location oLocation = oIterator.next();
            if (!equalsLocation(location, oLocation)) return false;
        }

        return true;
    }

    private boolean equalsLocation(Location location, Location oLocation) {
        if (location == null || oLocation == null) return location == oLocation;

        if (!equalsPhysicalLocation(location.getPhysicalLocation(), oLocation.getPhysicalLocation())) return false;
        if (location.getPhysicalLocation() != null) return true;
        Set<LogicalLocation> locations = location.getLogicalLocations();
        Set<LogicalLocation> oLocations = oLocation.getLogicalLocations();
        if (locations == null || oLocations == null) return locations == oLocations;
        if (locations.size() != oLocations.size()) return false;

        Iterator<LogicalLocation> iterator = locations.iterator();
        Iterator<LogicalLocation> oIterator = oLocations.iterator();
        while (iterator.hasNext()) {
            LogicalLocation logicalLocation = iterator.next();
            LogicalLocation oLogicalLocation = oIterator.next();
            if (!equalsLogicalLocation(logicalLocation, oLogicalLocation)) return false;
        }
        return true;
    }

    private boolean equalsLogicalLocation(LogicalLocation location, LogicalLocation oLocation) {
        if (location == null || oLocation == null) return location == oLocation;
        return Objects.equals(location.getName(), oLocation.getName()) &&
                Objects.equals(location.getKind(), oLocation.getKind());
    }

    private boolean equalsPhysicalLocation(PhysicalLocation location, PhysicalLocation oLocation) {
        if (location == null || oLocation == null) return location == oLocation;

        return equalsArtifactLocation(location.getArtifactLocation(), oLocation.getArtifactLocation()) &&
                equalsRegion(location.getRegion(), oLocation.getRegion());
    }

    private boolean equalsRegion(Region region, Region oRegion) {
        if (region == null || oRegion == null) return region == oRegion;

        return Objects.equals(region.getCharLength(), oRegion.getCharLength()) &&
                Objects.equals(region.getSnippet(), oRegion.getSnippet());
    }

    private boolean equalsArtifactLocation(ArtifactLocation artifactLocation, ArtifactLocation oArtifactLocation) {
        if (artifactLocation == null || oArtifactLocation == null) return artifactLocation == oArtifactLocation;

        return Objects.equals(artifactLocation.getUriBaseId(), oArtifactLocation.getUriBaseId()) &&
                Objects.equals(artifactLocation.getUri(), oArtifactLocation.getUri());
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = ((hash * 31) + ((result.getRuleId() == null) ? 0 : result.getRuleId().hashCode()));
        hash = ((hash * 31) + ((result.getMessage() == null) ? 0 : result.getMessage().hashCode()));
        hash = ((hash * 31) + ((result.getLevel() == null) ? 0 : result.getLevel().hashCode()));

        if (result.getLocations() == null) return hash;
        for (Location location : result.getLocations()) {
            PhysicalLocation physicalLocation = location.getPhysicalLocation();
            if (physicalLocation != null) {
                hash = ((hash * 31) + hashPhysicalLocation(physicalLocation));
            } else {
                Set<LogicalLocation> logicalLocations = location.getLogicalLocations();
                if (logicalLocations != null) {
                    for (LogicalLocation logicalLocation : logicalLocations) {
                        hash = ((hash * 31) + hashLogicalLocation(logicalLocation));
                    }
                }
            }
        }
        return hash;
    }


    public int hashPhysicalLocation(PhysicalLocation location) {
        if (location == null) return 0;
        int hash = 1;
        ArtifactLocation artifactLocation = location.getArtifactLocation();
        if (artifactLocation != null) {
            hash = ((hash * 31) + ((artifactLocation.getUri() == null) ? 0 : artifactLocation.getUri().hashCode()));
            hash = ((hash * 31) + ((artifactLocation.getUriBaseId() == null) ? 0 : artifactLocation.getUriBaseId().hashCode()));
        }
        hash = ((hash * 31) + hashRegion(location.getRegion()));
        return hash;
    }

    public int hashLogicalLocation(LogicalLocation location) {
        if (location == null) return 0;
        int hash = 1;
        hash = ((hash * 31) + ((location.getName() == null) ? 0 : location.getName().hashCode()));
        hash = ((hash * 31) + ((location.getKind() == null) ? 0 : location.getKind().hashCode()));
        return hash;
    }

    public int hashRegion(Region region) {
        if (region == null) return 0;
        int hash = 1;
        hash = ((hash * 31) + ((region.getCharLength() == null) ? 0 : region.getCharLength()));

        ArtifactContent snippet = region.getSnippet();
        if (snippet != null) {
            hash = ((hash * 31) + ((snippet.getText() == null) ? 0 : snippet.getText().hashCode()));
        }
        return hash;
    }
}