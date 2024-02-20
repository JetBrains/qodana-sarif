import com.google.gson.JsonSyntaxException
import com.jetbrains.qodana.sarif.RuleUtil
import com.jetbrains.qodana.sarif.SarifUtil
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation
import com.jetbrains.qodana.sarif.model.Invocation
import com.jetbrains.qodana.sarif.model.Run
import com.jetbrains.qodana.sarif.model.SarifReport
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists

object BaselineCli {
    fun process(
        reportPath: Path,
        baselinePath: Path?,
        failThreshold: Int?,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Int {
        if (reportPath.notExists()) {
            errPrinter("Please provide a valid SARIF report path")
            return ERROR_EXIT
        }
        val sarifReport = try {
            SarifUtil.readReport(reportPath)
        } catch (e: Exception) {
            errPrinter("Error reading SARIF report: ${e.message}")
            return ERROR_EXIT
        }

        val resolveInspectionName: (String) -> String = { id ->
            RuleUtil.findRuleDescriptor(sarifReport, id)?.shortDescription?.text ?: id
        }
        val printer = CommandLineResultsPrinter(simpleMemoize(resolveInspectionName), cliPrinter)
        return if (baselinePath != null) {
            compareBaselineThreshold(
                sarifReport,
                reportPath,
                baselinePath,
                failThreshold,
                printer,
                cliPrinter,
                errPrinter
            )
        } else {
            compareThreshold(sarifReport, reportPath, failThreshold, printer, cliPrinter, errPrinter)
        }
    }

    private fun processResultCount(
        size: Int,
        failThreshold: Int?,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Invocation {
        if (size > 0) {
            errPrinter("Found $size new problems according to the checks applied")
        } else {
            cliPrinter("It seems all right \uD83D\uDC4C No new problems found according to the checks applied")
        }
        if (failThreshold != null && size > failThreshold) {
            errPrinter("New problems count $size is greater than the threshold $failThreshold")
            return Invocation().apply {
                exitCode = THRESHOLD_EXIT
                exitCodeDescription = "Qodana reached failThreshold"
                executionSuccessful = true
            }
        }
        return Invocation().apply {
            exitCode = 0
            executionSuccessful = true
        }
    }

    private fun compareThreshold(
        sarifReport: SarifReport,
        sarifPath: Path,
        failThreshold: Int?,
        printer: CommandLineResultsPrinter,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Int {
        val results = sarifReport.runs.first().results
        printer.printResults(results, "Qodana - Detailed summary")
        val invocation = processResultCount(results.size, failThreshold, cliPrinter, errPrinter)
        sarifReport.runs.first().invocations = listOf(invocation)
        SarifUtil.writeReport(sarifPath, sarifReport)
        return invocation.exitCode
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
        printer.printResultsWithBaselineState(sarifReport.runs.first().results, false)
        val invocation = processResultCount(baselineCalculation.newResults, failThreshold, cliPrinter, errPrinter)
        sarifReport.runs.first().invocations = listOf(invocation)
        SarifUtil.writeReport(sarifPath, sarifReport)
        return invocation.exitCode
    }

    private fun createSarifReport(runs: List<Run>): SarifReport {
        val schema =
            URI("https://raw.githubusercontent.com/schemastore/schemastore/master/src/schemas/json/sarif-2.1.0-rtm.5.json")
        return SarifReport(SarifReport.Version._2_1_0, runs).`with$schema`(schema)
    }

    private fun <T : Any, R : Any> simpleMemoize(f: (T) -> R): (T) -> R {
        val state = mutableMapOf<T, R>()

        return { state.computeIfAbsent(it, f) }
    }
}
