package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation;
import com.jetbrains.qodana.sarif.model.Message;
import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.SarifReport;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jetbrains.qodana.sarif.baseline.BaselineCalculation.Options.DEFAULT;
import static org.junit.Assert.assertEquals;

public class BaselineTest {

    private static final String QODANA_REPORT_JSON = "src/test/resources/testData/readWriteTest/qodanaReport.json";

    @Test
    public void testSameReport() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();

        doTest(report, baseline, problemsCount(report), 0, 0);
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
    public void testDifferentToolName() throws IOException {
        SarifReport report = readReport();
        SarifReport baseline = readReport();
        Result newResult = new Result(new Message().withText("new result"));
        report.getRuns().get(0).getResults().add(newResult);

        baseline.getRuns().get(0).getTool().getDriver().setName("AnotherName");
        doTest(report, baseline, problemsCount(report) - 1, 0, 1);
        assertEquals(Result.BaselineState.NEW, newResult.getBaselineState());
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

        Map<Result.BaselineState, List<Result>> results = report.getRuns().get(0).getResults().stream().collect(Collectors.groupingBy(Result::getBaselineState));

        List<Result> resultsUnchanged = results.get(Result.BaselineState.UNCHANGED);
        List<Result> resultsAbsent = results.get(Result.BaselineState.ABSENT);
        List<Result> resultsNew = results.get(Result.BaselineState.NEW);
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
