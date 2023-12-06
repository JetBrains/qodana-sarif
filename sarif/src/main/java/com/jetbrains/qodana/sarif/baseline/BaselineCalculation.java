package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.*;
import kotlin.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static com.jetbrains.qodana.sarif.model.Result.BaselineState.*;

/**
 * Produces comparison of two reports. Fills baseline state.
 */
public class BaselineCalculation {
    public static final String EQUAL_INDICATOR = "equalIndicator";

    private int newResults = 0;
    private int absentResults = 0;
    private int unchangedResults = 0;
    private final Options options;

    private BaselineCalculation(Options options) {
        this.options = options;
    }

    public static BaselineCalculation compare(SarifReport report, SarifReport baseline, Options options) {
        BaselineCalculation result = new BaselineCalculation(options);
        result.fillBaselineState(report, baseline);

        return result;
    }

    public static BaselineCalculation compare(SarifReport report, SarifReport baseline) {
        return compare(report, baseline, Options.DEFAULT);
    }

    public int getNewResults() {
        return newResults;
    }

    public int getAbsentResults() {
        return absentResults;
    }

    public int getUnchangedResults() {
        return unchangedResults;
    }

    public void fillBaselineState(SarifReport report, SarifReport baseline) {
        List<Run> baselineRunsField = baseline.getRuns();
        List<Run> baselineRuns = baselineRunsField != null ? new ArrayList<>(baselineRunsField) : Collections.emptyList();

        List<Run> unmatched = new ArrayList<>();

        for (Run run : report.getRuns()) {
            Optional<Run> first =
                    baselineRuns.stream().filter((it) -> Objects.equals(getToolName(it), getToolName(run))).findFirst();

            if (first.isPresent()) {
                Run baselineRun = first.get();
                new RunResultGroup(run, baselineRun).build();
                baselineRuns.remove(baselineRun);
            } else {
                unmatched.add(run);
            }
        }

        for (int i = 0; i < unmatched.size(); i++) {
            Run run = unmatched.get(i);
            Run baselineRun = i < baselineRuns.size() ? baselineRuns.get(i) : null;
            if (baselineRun == null) {
                markRunAsNew(run);
                continue;
            }
            new RunResultGroup(run, baselineRun).build();
        }
    }

    private void markRunAsNew(Run run) {
        for (Result result : run.getResults()) {
            setBaselineState(result, NEW);
            newResults++;
        }
    }

    private String getToolName(Run run) {
        Tool tool = run.getTool();
        if (tool == null) return null;
        ToolComponent driver = tool.getDriver();
        if (driver == null) return null;
        return driver.getName();
    }

    public static class Options {
        public static final Options DEFAULT = new Options();

        private final boolean includeAbsent;
        private final boolean includeUnchanged;
        private final boolean fillBaselineState;

        /**
         * Provides information about incremental build.
         * This function applied to baseline result and should return true
         * if this result should be updated by current report.
         * It means:
         * if old result is absent and wasChecked == true -> oldResult.baselineState = ABSENT
         * if old result is absent and wasChecked == false -> oldResult.baselineState = UNCHANGED
         * if old result is present then current result `wasChecked` is not used.
         * <p>
         * Typically, wasChecked is true if result is in the scope of current check.
         */
        private static final Function<Result, Boolean> ALL_CHECKED = (result) -> true;
        private final Function<Result, Boolean> wasChecked;

        public Options() {
            includeAbsent = false;
            includeUnchanged = true;
            fillBaselineState = true;
            wasChecked = ALL_CHECKED;
        }

        public Options(boolean includeAbsent) {
            this(includeAbsent, true, true);
        }

        public Options(boolean includeAbsent, boolean includeUnchanged, boolean fillBaselineState) {
            this.includeAbsent = includeAbsent;
            this.includeUnchanged = includeUnchanged;
            this.fillBaselineState = fillBaselineState;
            wasChecked = ALL_CHECKED;
        }

        public Options(boolean includeAbsent,
                       boolean includeUnchanged,
                       boolean fillBaselineState,
                       Function<Result, Boolean> wasChecked) {
            this.includeAbsent = includeAbsent;
            this.includeUnchanged = includeUnchanged;
            this.fillBaselineState = fillBaselineState;
            this.wasChecked = wasChecked;
        }

        public boolean isIncludeAbsent() {
            return includeAbsent;
        }

        public boolean isIncludeUnchanged() {
            return includeUnchanged;
        }

        public boolean isFillBaselineState() {
            return fillBaselineState;
        }
    }

    private class RunResultGroup {
        private final MultiMap<String, Result> baselineHashes;
        private final MultiMap<String, Result> reportHashes;
        private final MultiMap<ResultKey, Result> diffBaseline;
        private final MultiMap<ResultKey, Result> diffReport;
        private final Run report;
        private final DescriptorLookup reportLookup;
        private final DescriptorLookup baselineLookup;

        public RunResultGroup(Run report, Run baseline) {
            this.report = report;
            this.reportLookup = new DescriptorLookup(report);
            this.baselineLookup = new DescriptorLookup(baseline);

            removeProblemsWithState(report, ABSENT);
            Pair<MultiMap<String, Result>, MultiMap<ResultKey, Result>> reportIndices =
                    BaselineKt.createIndices(report, m -> Collections.singletonList(m.getLastValue(EQUAL_INDICATOR)));

            Pair<MultiMap<String, Result>, MultiMap<ResultKey, Result>> baselineIndices =
                    BaselineKt.createIndices(baseline, m -> Collections.singletonList(m.getLastValue(EQUAL_INDICATOR)));
            this.reportHashes = reportIndices.getFirst();
            this.diffReport = reportIndices.getSecond();
            this.baselineHashes = baselineIndices.getFirst();
            this.diffBaseline = baselineIndices.getSecond();
        }

        private void removeProblemsWithState(Run report, Result.BaselineState state) {
            report.getResults().removeIf(result -> result.getBaselineState() == state);
        }

        public void build() {
            reportHashes.forEach(e -> {
                if (baselineHashes.containsKey(e.getKey())) {
                    e.getValue().forEach((it) -> setBaselineState(it, UNCHANGED));
                    unchangedResults += e.getValue().size();
                } else {
                    e.getValue().forEach((it) -> diffReport.add(new ResultKey(it), it));
                }
            });

            baselineHashes.forEach(e -> {
                if (!reportHashes.containsKey(e.getKey())) {
                    for (Result result : e.getValue()) {
                        if (options.wasChecked.apply(result)) {
                            diffBaseline.add(new ResultKey(result), result);
                        } else {
                            result.setBaselineState(UNCHANGED);
                            report.getResults().add(result);
                            unchangedResults += 1;
                        }
                    }
                }
            });

            diffReport.forEach(e -> {
                List<Result> baselineDiffBucket = diffBaseline.getOrEmpty(e.getKey());
                for (Result result : e.getValue()) {
                    if (baselineDiffBucket.isEmpty()) {
                        setBaselineState(result, NEW);
                        newResults++;
                    } else {
                        setBaselineState(result, UNCHANGED);
                        baselineDiffBucket.remove(baselineDiffBucket.size() - 1);
                        unchangedResults++;
                    }
                }
            });

            StreamSupport.stream(diffBaseline.spliterator(), false)
                    .flatMap((it) -> it.getValue().stream())
                    .forEach(result -> {
                        if (options.wasChecked.apply(result)) {
                            if (options.includeAbsent) {
                                setBaselineState(result, ABSENT);
                                absentResults++;
                                report.getResults().add(result);
                                if (reportLookup.findById(result.getRuleId()) == null) {
                                    DescriptorWithLocation descriptor = baselineLookup.findById(result.getRuleId());
                                    if (descriptor != null) descriptor.addTo(report);
                                }
                            }
                        } else {
                            result.setBaselineState(UNCHANGED);
                            report.getResults().add(result);
                            unchangedResults += 1;
                        }
                    });

            if (!options.includeUnchanged) {
                removeProblemsWithState(report, UNCHANGED);
                unchangedResults = 0;
            }

            if (!options.fillBaselineState) {
                for (Result result : report.getResults()) {
                    result.setBaselineState(null);
                }
            }
        }
    }

    private void setBaselineState(Result result, Result.BaselineState state) {
        result.setBaselineState(state);
    }
}
