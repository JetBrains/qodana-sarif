package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.ReportingDescriptor;
import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.Tool;

import java.util.*;
import java.util.stream.Stream;

final class DescriptorLookup {
    private final Run run;
    private Map<String, DescriptorWithLocation> index;

    DescriptorLookup(Run run) {
        this.run = run;
    }

    DescriptorWithLocation findById(String ruleId) {
        // Build a one-time snapshot index so repeated hits are O(1) instead of re-scanning every rule.
        // First-wins (putIfAbsent) reproduces the previous findFirst() semantics over the descriptor stream.
        if (index == null) {
            index = new HashMap<>();
            descriptors(run).forEach(d -> index.putIfAbsent(d.getDescriptor().getId(), d));
        }
        DescriptorWithLocation found = index.get(ruleId);
        if (found != null) return found;

        // A miss re-scans live: new descriptors might be added between calls (but never removed), so a
        // not-found result must not be cached as absent.
        found = descriptors(run)
                .filter(r -> Objects.equals(r.getDescriptor().getId(), ruleId))
                .findFirst()
                .orElse(null);
        if (found != null) index.put(ruleId, found);
        return found;
    }

    private Stream<DescriptorWithLocation> descriptors(Run run) {
        Stream<DescriptorWithLocation> driverRules = Optional.ofNullable(run.getTool())
                .map(Tool::getDriver)
                .map(driver -> {
                    Stream<ReportingDescriptor> s = driver.getRules() == null ? Stream.empty() : driver.getRules().stream();
                    return s.map(r -> new DescriptorWithLocation(r, driver));
                })
                .orElseGet(Stream::empty);

        Stream<DescriptorWithLocation> extRules = Optional.ofNullable(run.getTool())
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
