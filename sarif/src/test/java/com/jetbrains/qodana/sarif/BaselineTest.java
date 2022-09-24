package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation;
import com.jetbrains.qodana.sarif.model.Message;
import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.SarifReport;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options.DEFAULT;
import static org.junit.Assert.assertEquals;

public class BaselineTest {

    private static final String QODANA_REPORT_JSON = "src/test/resources/testData/readWriteTest/qodanaReport.json";
    private static final String QODANA_REPORT_JSON_2 = "src/test/resources/testData/readWriteTest/qodanaReport2.json";

    @Test
    public void testSameReport() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();

        doTest(report, baseline, problemsCount(report), 0, 0);
    }

    @Test
    public void testSameReportNotFillState() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();

        doTest(report, baseline, problemsCount(report), 0, 0, new BaselineCalculation.Options(false, true, false));
    }

    @Test
    public void testSameReportNotFillStateAndUnchanged() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();

        doTest(report, baseline, 0, 0, 0, new BaselineCalculation.Options(false, false, false));
    }

    @Test
    public void testCompareWithEmpty() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = new SarifReport();

        doTest(report, baseline, 0, 0, problemsCount(report));
    }

    @Test
    public void testCompareWithOneAbsent() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result newResult = new Result(new Message().withText("new result"));
        baseline.getRuns().get(0).getResults().add(newResult);

        doTest(report, baseline, problemsCount(report), 1, 0, new BaselineCalculation.Options(true));
        assertEquals(Result.BaselineState.ABSENT, newResult.getBaselineState());
    }

    @Test
    public void testCompareWithOneAbsentInBaseline() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result newResult = new Result(new Message().withText("new result"));
        baseline.getRuns().get(0).getResults().add(newResult);

        BaselineCalculation.compare(report, baseline, new BaselineCalculation.Options(true));

        SarifReport newReport = readReport();

        doTest(newReport, report, problemsCount(newReport), 0, 0, new BaselineCalculation.Options(true));
    }

    @Test
    public void testCompareWithOneAbsentInReport() throws IOException {
        SarifReport baseline = readReport();
        String toolName = baseline.getRuns().get(0).getTool().getDriver().getName();
        SarifReport report = SarifUtil.emptyReport(toolName);
        Result newResult = new Result(new Message().withText("new result"));
        baseline.getRuns().get(0).getResults().add(newResult);


        doTest(report, baseline, 0, problemsCount(baseline), 0, new BaselineCalculation.Options(true));

        SarifReport newBaseline = SarifUtil.emptyReport(toolName);

        doTest(report, newBaseline, 0, 0, 0, new BaselineCalculation.Options(true));
    }

    @Test
    public void testCompareWithOneNew() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result newResult = new Result(new Message().withText("new result"));
        report.getRuns().get(0).getResults().add(newResult);

        doTest(report, baseline, problemsCount(report) - 1, 0, 1);
        assertEquals(Result.BaselineState.NEW, newResult.getBaselineState());
    }

    @Test
    public void testCompareWithOneNewIgnoreUnchanged() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result newResult = new Result(new Message().withText("new result"));
        report.getRuns().get(0).getResults().add(newResult);

        doTest(report, baseline, 0, 0, 1, new BaselineCalculation.Options(false, false, true));
        assertEquals(Result.BaselineState.NEW, newResult.getBaselineState());
        assertEquals(1, report.getRuns().get(0).getResults().size());
    }

    @Test
    public void testIncrementalBaselineOneNew() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result newResult = new Result(new Message().withText("new result"));
        report.getRuns().get(0).setResults(new ArrayList<>(List.of(newResult)));

        BaselineCalculation.Options options = new BaselineCalculation.Options(true, true, true,
                (result -> result == newResult));
        doTest(report, baseline, problemsCount(baseline), 0, 1, options);
        assertEquals(Result.BaselineState.NEW, newResult.getBaselineState());
        assertEquals(problemsCount(baseline) + 1, report.getRuns().get(0).getResults().size());
    }

    @Test
    public void testIncrementalBaselineOneAbsent() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result newResult = new Result(new Message().withText("new result"));
        baseline.getRuns().get(0).setResults(new ArrayList<>(List.of(newResult)));

        BaselineCalculation.Options options = new BaselineCalculation.Options(true, true, true,
                (result -> result == newResult));
        doTest(report, baseline, 0, 1, problemsCount(report), options);
        assertEquals(Result.BaselineState.ABSENT, newResult.getBaselineState());
    }

    @Test
    public void testIncrementalBaselineOneAbsentOneNew() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result result1 = new Result(new Message().withText("new result1"));
        Result result2 = new Result(new Message().withText("new result2"));
        Result result3 = new Result(new Message().withText("new result3"));
        Result result4 = new Result(new Message().withText("new result4"));
        baseline.getRuns().get(0).setResults(new ArrayList<>(List.of(result1, result2, result4)));
        report.getRuns().get(0).setResults(new ArrayList<>(List.of(result2, result3)));

        BaselineCalculation.Options options = new BaselineCalculation.Options(true, true, true,
                (result -> result == result1 || result == result2 || result == result3));

        doTest(report, baseline, 2, 1, 1, options);
        assertEquals(Result.BaselineState.ABSENT, result1.getBaselineState());
        assertEquals(Result.BaselineState.UNCHANGED, result2.getBaselineState());
        assertEquals(Result.BaselineState.NEW, result3.getBaselineState());
        assertEquals(Result.BaselineState.UNCHANGED, result4.getBaselineState());
    }


    @Test
    public void testDifferentToolName() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport(QODANA_REPORT_JSON_2);
        int problemsCount = problemsCount(report);
        doTest(report, baseline, problemsCount, 1, 0, new BaselineCalculation.Options(true));
        //assertEquals(Result.BaselineState.NEW, newResult.getBaselineState());
    }

    private void doTest(SarifReport report,
                        SarifReport baseline,
                        int expectedUnchanged,
                        int expectedAbsent,
                        int expectedNew,
                        BaselineCalculation.Options options
    ) {
        BaselineCalculation calculation = BaselineCalculation.compare(report, baseline, options);
        assertEquals("Unchanged:", expectedUnchanged, calculation.getUnchangedResults());
        assertEquals("Absent:", expectedAbsent, calculation.getAbsentResults());
        assertEquals("New:", expectedNew, calculation.getNewResults());
        List<Result> results = report.getRuns().get(0).getResults();

        if (!options.isFillBaselineState()) {
            long count = results.stream().filter(it -> it.getBaselineState() == null).count();
            assertEquals(results.size(), count);
            assertEquals(expectedUnchanged + expectedAbsent + expectedNew, count);
            return;
        }

        Map<Result.BaselineState, List<Result>> grouped = results.stream()
                .filter(it -> it.getBaselineState() != null)
                .collect(Collectors.groupingBy(Result::getBaselineState));

        List<Result> resultsUnchanged = grouped.get(Result.BaselineState.UNCHANGED);
        List<Result> resultsAbsent = grouped.get(Result.BaselineState.ABSENT);
        List<Result> resultsNew = grouped.get(Result.BaselineState.NEW);
        assertEquals("Unchanged:", expectedUnchanged, resultsUnchanged == null ? 0 : resultsUnchanged.size());
        assertEquals("Absent:", expectedAbsent, resultsAbsent == null ? 0 : resultsAbsent.size());
        assertEquals("New:", expectedNew, resultsNew == null ? 0 : resultsNew.size());
    }

    private void doTest(SarifReport report,
                        SarifReport baseline,
                        int expectedUnchanged,
                        int expectedAbsent,
                        int expectedNew
    ) {
        doTest(report, baseline, expectedUnchanged, expectedAbsent, expectedNew, DEFAULT);
    }

    private static int problemsCount(SarifReport report) {
        return report.getRuns().stream().mapToInt(it -> it.getResults().size()).sum();
    }

    private static SarifReport readReport() throws IOException {
        return readReport(QODANA_REPORT_JSON);
    }

    private static SarifReport readReport(String path) throws IOException {
        Path reportPath = Paths.get(path);
        return SarifUtil.readReport(reportPath);
    }
}
