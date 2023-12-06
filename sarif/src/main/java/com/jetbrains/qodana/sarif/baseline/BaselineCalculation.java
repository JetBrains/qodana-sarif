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

    int newResults = 0;
    int absentResults = 0;
    int unchangedResults = 0;
    final Options options;

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
                new RunResultGroup(this, run, baselineRun).build();
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
            new RunResultGroup(this, run, baselineRun).build();
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

        final boolean includeAbsent;
        final boolean includeUnchanged;
        final boolean fillBaselineState;

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
        final Function<Result, Boolean> wasChecked;

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
}
