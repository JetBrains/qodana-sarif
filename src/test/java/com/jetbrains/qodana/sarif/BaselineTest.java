package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation;
import com.jetbrains.qodana.sarif.model.Message;
import com.jetbrains.qodana.sarif.model.Result;
import com.jetbrains.qodana.sarif.model.SarifReport;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        doTest(report, baseline, problemsCount(report), 1, 0);
        assertEquals(Result.BaselineState.ABSENT, newResult.getBaselineState());
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

    private void doTest(SarifReport report,
                        SarifReport baseline,
                        int expectedUnchanged,
                        int expectedAbsent,
                        int expectedNew
    ) {
        BaselineCalculation calculation = BaselineCalculation.compare(report, baseline);
        assertEquals("Unchanged:", expectedUnchanged, calculation.getUnchangedResults());
        assertEquals("Absent:", expectedAbsent, calculation.getAbsentResults());
        assertEquals("New", expectedNew, calculation.getNewResults());
    }

    private static int problemsCount(SarifReport report) {
        return report.getRuns().stream().mapToInt(it -> it.getResults().size()).sum();
    }

    private static SarifReport readReport() throws IOException {
        Path reportPath = Paths.get(QODANA_REPORT_JSON);
        return SarifUtil.readReport(reportPath);
    }
}
