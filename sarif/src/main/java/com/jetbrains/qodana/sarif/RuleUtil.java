package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public class RuleUtil {
    private RuleUtil() {
        throw new IllegalStateException("No instances.");
    }

    @Nullable
    public static ReportingDescriptor findRuleDescriptor(@NotNull SarifReport report, @Nullable String ruleId) {
        if (ruleId == null || ruleId.isEmpty()) return null;

        return allRules(report)
                .filter(r -> Objects.equals(r.getId(), ruleId))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public static Stream<ReportingDescriptor> allRules(@NotNull SarifReport report) {
        return stream(report.getRuns())
                .map(Run::getTool)
                .filter(Objects::nonNull)
                .flatMap(tool -> {
                    ToolComponent driver = tool.getDriver();

                    Stream<ReportingDescriptor> driverRules = driver == null ? Stream.empty() : stream(driver.getRules());
                    Stream<ReportingDescriptor> extRules = stream(tool.getExtensions())
                            .flatMap(e -> stream(e.getRules()));

                    return Stream.concat(driverRules, extRules);
                });
    }

    @NotNull
    private static <T> Stream<T> stream(@Nullable Collection<T> c) {
        if (c == null) return Stream.empty();
        else return c.stream().filter(Objects::nonNull);
    }
}
