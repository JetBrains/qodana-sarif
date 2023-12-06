package com.jetbrains.qodana.sarif.baseline;

import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.Run;
import kotlin.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.jetbrains.qodana.sarif.model.Result.BaselineState.*;

class RunResultGroup {
    private final BaselineCalculation baselineCalculation;
    private final MultiMap<String, Result> baselineHashes;
    private final MultiMap<String, Result> reportHashes;
    private final MultiMap<ResultKey, Result> diffBaseline;
    private final MultiMap<ResultKey, Result> diffReport;
    private final Run report;
    private final DescriptorLookup reportLookup;
    private final DescriptorLookup baselineLookup;


    public RunResultGroup(BaselineCalculation baselineCalculation, Run report, Run baseline) {
        this.baselineCalculation = baselineCalculation;
        this.report = report;
        this.reportLookup = new DescriptorLookup(report);
        this.baselineLookup = new DescriptorLookup(baseline);

        removeProblemsWithState(report, ABSENT);
        Pair<MultiMap<String, Result>, MultiMap<ResultKey, Result>> reportIndices =
                BaselineKt.createIndices(report, m -> Collections.singletonList(m.getLastValue(BaselineCalculation.EQUAL_INDICATOR)));

        Pair<MultiMap<String, Result>, MultiMap<ResultKey, Result>> baselineIndices =
                BaselineKt.createIndices(baseline, m -> Collections.singletonList(m.getLastValue(BaselineCalculation.EQUAL_INDICATOR)));
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
                e.getValue().forEach((it) -> it.setBaselineState(UNCHANGED));
                baselineCalculation.unchangedResults += e.getValue().size();
            } else {
                e.getValue().forEach((it) -> diffReport.add(new ResultKey(it), it));
            }
        });

        baselineHashes.forEach(e -> {
            if (!reportHashes.containsKey(e.getKey())) {
                for (Result result : e.getValue()) {
                    if (baselineCalculation.options.wasChecked.apply(result)) {
                        diffBaseline.add(new ResultKey(result), result);
                    } else {
                        result.setBaselineState(UNCHANGED);
                        report.getResults().add(result);
                        baselineCalculation.unchangedResults += 1;
                    }
                }
            }
        });

        diffReport.forEach(e -> {
            List<Result> baselineDiffBucket = diffBaseline.getOrEmpty(e.getKey());
            for (Result result : e.getValue()) {
                if (baselineDiffBucket.isEmpty()) {
                    result.setBaselineState(NEW);
                    baselineCalculation.newResults++;
                } else {
                    result.setBaselineState(UNCHANGED);
                    baselineDiffBucket.remove(baselineDiffBucket.size() - 1);
                    baselineCalculation.unchangedResults++;
                }
            }
        });

        StreamSupport.stream(diffBaseline.spliterator(), false)
                .flatMap((it) -> it.getValue().stream())
                .forEach(result -> {
                    if (baselineCalculation.options.wasChecked.apply(result)) {
                        if (baselineCalculation.options.includeAbsent) {
                            result.setBaselineState(ABSENT);
                            baselineCalculation.absentResults++;
                            report.getResults().add(result);
                            if (reportLookup.findById(result.getRuleId()) == null) {
                                DescriptorWithLocation descriptor = baselineLookup.findById(result.getRuleId());
                                if (descriptor != null) descriptor.addTo(report);
                            }
                        }
                    } else {
                        result.setBaselineState(UNCHANGED);
                        report.getResults().add(result);
                        baselineCalculation.unchangedResults += 1;
                    }
                });

        if (!baselineCalculation.options.includeUnchanged) {
            removeProblemsWithState(report, UNCHANGED);
            baselineCalculation.unchangedResults = 0;
        }

        if (!baselineCalculation.options.fillBaselineState) {
            for (Result result : report.getResults()) {
                result.setBaselineState(null);
            }
        }
    }
}
