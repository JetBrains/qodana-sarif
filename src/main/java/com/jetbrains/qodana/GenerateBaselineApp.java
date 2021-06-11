package com.jetbrains.qodana;

import com.jetbrains.qodana.sarif.SarifUtil;
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation;
import com.jetbrains.qodana.sarif.model.SarifReport;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class GenerateBaselineApp {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Format: %reportPath% %baselinePath% %outFilename%[--failOnNew] [--failOnAbsent]");
            System.exit(1);
        }
        Path reportPath = Paths.get(args[0]);
        Path baselinePath = Paths.get(args[1]);
        String outFilename = args[2];
        SarifReport report = SarifUtil.readReport(reportPath);
        SarifReport baseline = SarifUtil.readReport(baselinePath);
        System.out.println("Starting baseline calculation. Report: " + reportPath.toAbsolutePath() + "; Baseline: " + baselinePath.toAbsolutePath());
        BaselineCalculation calculation = BaselineCalculation.compare(report, baseline);
        System.out.printf(
                "Baseline comparison result - UNCHANGED: %s, NEW: %s, ABSENT: %s\n",
                calculation.getUnchangedResults(),
                calculation.getNewResults(),
                calculation.getAbsentResults()
        );

        Path outPath = Paths.get(outFilename);
        System.out.println("Writing report with baseline comparison to: " + outPath.toAbsolutePath());
        SarifUtil.writeReport(outPath, report);
        if (Arrays.asList(args).contains("--failOnNew") && calculation.getNewResults() > 0) System.exit(255);
        if (Arrays.asList(args).contains("--failOnAbsent") && calculation.getAbsentResults() > 0) System.exit(255);
    }
}
