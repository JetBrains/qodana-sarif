package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.*;

import java.util.*;
import java.util.function.Function;

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
        private final Map<String, List<Result>> baselineHashes = new HashMap<>();
        private final Map<String, List<Result>> reportHashes = new HashMap<>();
        private final Map<ResultKey, List<Result>> diffBaseline = new HashMap<>();
        private final Map<ResultKey, List<Result>> diffReport = new HashMap<>();
        private final Run report;

        public RunResultGroup(Run report, Run baseline) {
            this.report = report;
            buildMap(baseline, baselineHashes, diffBaseline);
            removeProblemsWithState(report, ABSENT);
            buildMap(report, reportHashes, diffReport);
        }

        private void removeProblemsWithState(Run report, Result.BaselineState state) {
            report.getResults().removeIf(result -> result.getBaselineState() == state);
        }

        private void buildMap(Run run, Map<String, List<Result>> map, Map<ResultKey, List<Result>> diffSet) {
            for (Result result : run.getResults()) {
                if (result.getBaselineState() == ABSENT) continue;
                VersionedMap<String> fingerprints = result.getPartialFingerprints();
                String equalIndicator = fingerprints != null ? fingerprints.getLastValue(EQUAL_INDICATOR) : null;
                if (equalIndicator != null) {
                    List<Result> resultBucket = map.compute(
                            equalIndicator,
                            (key, value) -> value != null ? value : new ArrayList<>());
                    resultBucket.add(result);
                } else {
                    addToDiff(result, diffSet);
                }
            }
        }

        public void addToDiff(Result result, Map<ResultKey, List<Result>> diffSet) {
            List<Result> resultBucket = diffSet.compute(
                    new ResultKey(result),
                    (key, value) -> value != null ? value : new ArrayList<>()
            );
            resultBucket.add(result);
        }

        public void build() {
            reportHashes.forEach((hash, results) -> {
                if (baselineHashes.containsKey(hash)) {
                    results.forEach((it) -> setBaselineState(it, UNCHANGED));
                    unchangedResults += results.size();
                } else {
                    results.forEach((it) -> addToDiff(it, diffReport));
                }
            });

            baselineHashes.forEach((hash, results) -> {
                if (!reportHashes.containsKey(hash)) {
                    for (Result result : results) {
                        if (options.wasChecked.apply(result)) {
                            addToDiff(result, diffBaseline);
                        } else {
                            result.setBaselineState(UNCHANGED);
                            report.getResults().add(result);
                            unchangedResults += 1;
                        }
                    }
                }
            });

            diffReport.forEach((key, reportDiffBucket) -> {
                List<Result> baselineDiffBucket = diffBaseline.getOrDefault(key, Collections.emptyList());
                for (Result result : reportDiffBucket) {
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

            diffBaseline.entrySet().stream().flatMap((it) -> it.getValue().stream()).forEach(result -> {
                if (options.wasChecked.apply(result)) {
                    if (options.includeAbsent) {
                        setBaselineState(result, ABSENT);
                        absentResults++;
                        report.getResults().add(result);
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



