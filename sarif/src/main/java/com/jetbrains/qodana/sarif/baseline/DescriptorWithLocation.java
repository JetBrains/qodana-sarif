package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.ReportingDescriptor;
import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.Tool;
import com.jetbrains.qodana.sarif.model.ToolComponent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class DescriptorWithLocation {
    @NotNull
    private final ReportingDescriptor descriptor;
    @NotNull
    private final ToolComponent location;

    public DescriptorWithLocation(
            @NotNull ReportingDescriptor descriptor,
            @NotNull ToolComponent location) {
        this.descriptor = descriptor;
        this.location = location;
    }

    @NotNull
    ReportingDescriptor getDescriptor() {
        return descriptor;
    }

    void addTo(Run run) {
        Tool tool = run.getTool();
        if (tool == null) return;
        Stream<ToolComponent> fromDriver = Optional.ofNullable(tool.getDriver())
                .map(Stream::of)
                .orElseGet(Stream::empty);

        Stream<ToolComponent> fromExtensions = Optional.ofNullable(tool.getExtensions())
                .map(Collection::stream)
                .orElseGet(Stream::empty);

        Optional<ToolComponent> existing = Stream.concat(fromDriver, fromExtensions)
                .filter(e -> Objects.equals(e.getFullName(), location.getFullName()) &&
                        Objects.equals(e.getVersion(), location.getVersion())
                )
                .findFirst();

        if (existing.isPresent()) {
            ToolComponent e = existing.get();
            getOrCreate(e::getRules, e::setRules, ArrayList::new)
                    .add(descriptor);
        } else {
            // Collections.singletonList() is immutable
            //noinspection ArraysAsListWithZeroOrOneArgument
            getOrCreate(tool::getExtensions, tool::setExtensions, HashSet::new)
                    // we don't want to copy all rules from the source AND not override them
                    .add(location.shallowCopy()
                            .withIsComprehensive(false)
                            .withRules(Arrays.asList(descriptor))
                    );
        }

    }

    private <T> @NotNull T getOrCreate(Supplier<T> get, Consumer<T> set, Supplier<T> create) {
        T value = get.get();
        if (value != null) return value;
        value = create.get();
        set.accept(value);
        return value;
    }

}
