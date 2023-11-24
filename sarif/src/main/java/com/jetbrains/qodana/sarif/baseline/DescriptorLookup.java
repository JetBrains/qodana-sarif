package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.ReportingDescriptor;
import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.Tool;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class DescriptorLookup {
    private final Map<String, DescriptorWithLocation> lookup;
    private final Supplier<Stream<DescriptorWithLocation>> descriptors;

    DescriptorLookup(Run run) {
        this.lookup = new HashMap<>();
        this.descriptors = () -> descriptors(run);
    }

    DescriptorWithLocation findById(String ruleId) {
        // don't cache not-found results because new descriptors might be added between calls (but not removed)
        return lookup.computeIfAbsent(ruleId, id ->
                descriptors.get()
                        .filter(r -> Objects.equals(r.getDescriptor().getId(), id))
                        .findFirst()
                        .orElse(null)
        );
    }

    private Stream<DescriptorWithLocation> descriptors(Run run) {
        Stream<DescriptorWithLocation> driverRules = Optional.ofNullable(run.getTool())
                .map(Tool::getDriver)
                .map(driver -> {
                    Stream<ReportingDescriptor> s = driver.getRules() == null ? Stream.empty() : driver.getRules().stream();
                    return s.map(r -> new DescriptorWithLocation(r, driver));
                })
                .orElseGet(Stream::empty);

        Stream<DescriptorWithLocation> extRules = Optional.of(run.getTool())
                .map(Tool::getExtensions)
                .orElseGet(HashSet::new)
                .stream()
                .flatMap(extension -> {
                    Stream<ReportingDescriptor> s = extension.getRules() == null ? Stream.empty() : extension.getRules().stream();
                    return s.map(r -> new DescriptorWithLocation(r, extension));
                });

        return Stream.concat(driverRules, extRules);
    }

}
