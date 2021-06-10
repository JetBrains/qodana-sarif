package com.jetbrains.qodana;

import com.jetbrains.qodana.sarif.SarifUtil;
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation;
import com.jetbrains.qodana.sarif.model.SarifReport;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class GenerateBaselineApp {
    public static void main(String[] args) throws IOException {
        if (args.length < 3 ) System.out.println("Format: %reportFilename% %baselineFilename% %outFilename%[--failOnNew] [--failOnAbsent]");
        String reportFilename = args[0];
        String baselineFilename = args[1];
        String outFilename = args[1];
        SarifReport report = SarifUtil.readReport(Paths.get(reportFilename));
        SarifReport baseline = SarifUtil.readReport(Paths.get(baselineFilename));
        BaselineCalculation baselineResult = BaselineCalculation.compare(report, baseline);
        SarifUtil.writeReport(Paths.get(outFilename), report);
        if (Arrays.asList(args).contains("--failOnNew") && baselineResult.getNewResults() > 0) System.exit(255);
        if (Arrays.asList(args).contains("--failOnAbsent") && baselineResult.getAbsentResults() > 0) System.exit(255);
    }
}
