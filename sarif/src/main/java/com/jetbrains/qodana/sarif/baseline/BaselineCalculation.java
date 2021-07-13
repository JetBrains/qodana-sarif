package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.*;

import java.util.*;

import static com.jetbrains.qodana.sarif.model.Result.BaselineState.*;

/**
 * Produces comparison of two reports. Fills baseline state.
 */
public class BaselineCalculation {
    public static final String EQUAL_INDICATOR = "equalIndicator";

    private int newResults = 0;
    private int absentResults = 0;
    private int unchangedResults = 0;
    private Options options;

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
        List<Run> baselineRuns = baseline.getRuns();
        for (Run run : report.getRuns()) {
            Optional<Run> first = baselineRuns == null ?
                    Optional.empty() :
                    baselineRuns.stream().filter((it) -> Objects.equals(getToolName(it), getToolName(run))).findFirst();

            if (first.isPresent()) {
                new RunResultGroup(run, first.get()).build();
            } else {
                markRunAsNew(run);
            }
        }
    }

    private void markRunAsNew(Run run) {
        for (Result result : run.getResults()) {
            result.setBaselineState(NEW);
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

        public Options() {
            includeAbsent = false;
        }

        public Options(boolean includeAbsent) {
            this.includeAbsent = includeAbsent;
        }
    }

    private class RunResultGroup {
        private final Map<String, Result> baselineHashes = new HashMap<>();
        private final Map<String, Result> reportHashes = new HashMap<>();
        private final Map<ResultKey, List<Result>> diffBaseline = new HashMap<>();
        private final Map<ResultKey, List<Result>> diffReport = new HashMap<>();
        private final Run report;

        public RunResultGroup(Run report, Run baseline) {
            this.report = report;
            buildMap(baseline, baselineHashes, diffBaseline);
            buildMap(report, reportHashes, diffReport);

        }

        private void buildMap(Run run, Map<String, Result> map, Map<ResultKey, List<Result>> diffSet) {
            for (Result result : run.getResults()) {
                VersionedMap<String> fingerprints = result.getPartialFingerprints();
                String equalIndicator = fingerprints != null ? fingerprints.getLastValue(EQUAL_INDICATOR) : null;
                if (equalIndicator != null) {
                    map.put(equalIndicator, result);
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
            reportHashes.forEach((hash, result) -> {
                if (baselineHashes.containsKey(hash)) {
                    result.setBaselineState(UNCHANGED);
                    unchangedResults++;
                } else {
                    addToDiff(result, diffReport);
                }
            });

            baselineHashes.forEach((hash, result) -> {
                if (!reportHashes.containsKey(hash)) {
                    addToDiff(result, diffBaseline);
                }
            });

            diffReport.forEach((key, reportDiffBucket) -> {
                List<Result> baselineDiffBucket = diffBaseline.getOrDefault(key, Collections.emptyList());
                for (Result result : reportDiffBucket) {
                    if (baselineDiffBucket.isEmpty()) {
                        result.setBaselineState(NEW);
                        newResults++;
                    } else {
                        result.setBaselineState(UNCHANGED);
                        baselineDiffBucket.remove(baselineDiffBucket.size() - 1);
                        unchangedResults++;
                    }
                }
            });

            if (options.includeAbsent) {
                diffBaseline.entrySet().stream().flatMap((it) -> it.getValue().stream()).forEach(result -> {
                    result.setBaselineState(ABSENT);
                    absentResults++;
                    report.getResults().add(result);
                });
            }
        }
    }
}



