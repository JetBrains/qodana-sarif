import com.jetbrains.qodana.sarif.SarifUtil
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation
import com.jetbrains.qodana.sarif.model.SarifReport
import java.nio.file.Files
import java.nio.file.Path

object BaselineCli {
    fun process(map: Map<String, String>, cliPrinter: (String) -> Unit): Int {
        val sarifPath = map["sarifReport"]!!
        val baselinePath = map["baselineReport"]
        val failThreshold = map["failThreshold"]?.toIntOrNull()
        if (!Files.exists(Path.of(sarifPath))) {
            cliPrinter("Please provide a valid SARIF report path")
            return ERROR_EXIT
        }
        val sarifReport: SarifReport
        try {
            sarifReport = SarifUtil.readReport(Path.of(sarifPath))
        } catch (e: Exception) {
            cliPrinter("Error reading SARIF report: ${e.message}")
            return ERROR_EXIT
        }
        val printer = CommandLineResultsPrinter( { it }, cliPrinter)
        return if (baselinePath != null) {
            compareBaselineThreshold(sarifReport, Path.of(sarifPath), Path.of(baselinePath), failThreshold, printer, cliPrinter)
        } else {
            compareThreshold(sarifReport, failThreshold, printer, cliPrinter)
        }
    }
    private fun compareThreshold(sarifReport: SarifReport, failThreshold: Int?, printer: CommandLineResultsPrinter, cliPrinter: (String) -> Unit): Int {
        val results = sarifReport.runs.first().results
        printer.printResults(results, "Qodana - Detailed summary")
        if (failThreshold != null && results.size > failThreshold) {
            cliPrinter("New problems count ${results.size} is greater than the threshold $failThreshold")
            return THRESHOLD_EXIT
        }
        return 0
    }

    private fun compareBaselineThreshold(sarifReport: SarifReport, sarifPath: Path, baselinePath: Path, failThreshold: Int?, printer: CommandLineResultsPrinter, cliPrinter: (String) -> Unit): Int {
        if (!Files.exists(baselinePath)) {
            cliPrinter("Please provide valid baseline report path")
            return ERROR_EXIT
        }
        val baseline: SarifReport
        try {
            baseline = SarifUtil.readReport(baselinePath)
        } catch (e: Exception) {
            cliPrinter("Error reading baseline report: ${e.message}")
            return ERROR_EXIT
        }
        val baselineCalculation = BaselineCalculation.compare(sarifReport, baseline, BaselineCalculation.Options())
        SarifUtil.writeReport(sarifPath, sarifReport)
        printer.printResultsWithBaselineState(sarifReport.runs.first().results, false)
        if (failThreshold != null && baselineCalculation.newResults > failThreshold) {
            cliPrinter("New problems count ${baselineCalculation.newResults} is greater than the threshold $failThreshold")
            return THRESHOLD_EXIT
        }
        return 0
    }
}