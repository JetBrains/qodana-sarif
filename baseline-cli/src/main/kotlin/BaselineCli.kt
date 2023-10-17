import com.google.gson.JsonSyntaxException
import com.jetbrains.qodana.sarif.SarifUtil
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation
import com.jetbrains.qodana.sarif.model.Run
import com.jetbrains.qodana.sarif.model.SarifReport
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object BaselineCli {
    fun process(map: Map<String, String>, cliPrinter: (String) -> Unit, errPrinter: (String) -> Unit): Int {
        val sarifPath = map["sarifReport"]!!
        val baselinePath = map["baselineReport"]
        val failThreshold = map["failThreshold"]?.toIntOrNull()
        if (!Files.exists(Paths.get(sarifPath))) {
            errPrinter("Please provide a valid SARIF report path")
            return ERROR_EXIT
        }
        val sarifReport: SarifReport
        try {
            sarifReport = SarifUtil.readReport(Paths.get(sarifPath))
        } catch (e: Exception) {
            errPrinter("Error reading SARIF report: ${e.message}")
            return ERROR_EXIT
        }
        val printer = CommandLineResultsPrinter({ it }, cliPrinter)
        return if (baselinePath != null) {
            compareBaselineThreshold(
                sarifReport,
                Paths.get(sarifPath),
                Paths.get(baselinePath),
                failThreshold,
                printer,
                cliPrinter,
                errPrinter
            )
        } else {
            compareThreshold(sarifReport, failThreshold, printer, cliPrinter, errPrinter)
        }
    }

    private fun processResultCount(
        size: Int,
        failThreshold: Int?,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Int {
        if (size > 0) {
            errPrinter("Found $size new problems according to the checks applied")
        } else {
            cliPrinter("It seems all right \uD83D\uDC4C No new problems found according to the checks applied")
        }
        if (failThreshold != null && size > failThreshold) {
            errPrinter("New problems count $size is greater than the threshold $failThreshold")
            return THRESHOLD_EXIT
        }
        return 0
    }

    private fun compareThreshold(
        sarifReport: SarifReport,
        failThreshold: Int?,
        printer: CommandLineResultsPrinter,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Int {
        val results = sarifReport.runs.first().results
        printer.printResults(results, "Qodana - Detailed summary")
        return processResultCount(results.size, failThreshold, cliPrinter, errPrinter)
    }

    private fun compareBaselineThreshold(
        sarifReport: SarifReport,
        sarifPath: Path,
        baselinePath: Path,
        failThreshold: Int?,
        printer: CommandLineResultsPrinter,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Int {
        if (!Files.exists(baselinePath)) {
            errPrinter("Please provide valid baseline report path")
            return ERROR_EXIT
        }
        val baseline: SarifReport
        try {
            baseline = SarifUtil.readReport(baselinePath) ?: createSarifReport(emptyList())
        } catch (e: JsonSyntaxException) {
            errPrinter("Error reading baseline report: ${e.message}")
            return ERROR_EXIT
        }
        val baselineCalculation = BaselineCalculation.compare(sarifReport, baseline, BaselineCalculation.Options())
        SarifUtil.writeReport(sarifPath, sarifReport)
        printer.printResultsWithBaselineState(sarifReport.runs.first().results, false)
        return processResultCount(baselineCalculation.newResults, failThreshold, cliPrinter, errPrinter)
    }

    private fun createSarifReport(runs: List<Run>): SarifReport {
        val schema =
            URI("https://raw.githubusercontent.com/schemastore/schemastore/master/src/schemas/json/sarif-2.1.0-rtm.5.json")
        return SarifReport(SarifReport.Version._2_1_0, runs).`with$schema`(schema)
    }
}