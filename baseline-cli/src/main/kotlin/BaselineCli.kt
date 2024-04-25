import Severity.Companion.severity
import com.google.gson.JsonSyntaxException
import com.jetbrains.qodana.sarif.RuleUtil
import com.jetbrains.qodana.sarif.SarifUtil
import com.jetbrains.qodana.sarif.baseline.BaselineCalculation
import com.jetbrains.qodana.sarif.model.Invocation
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Run
import com.jetbrains.qodana.sarif.model.SarifReport
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object BaselineCli {
    fun process(options: BaselineOptions, cliPrinter: (String) -> Unit, errPrinter: (String) -> Unit): Int {
        if (!Files.exists(Paths.get(options.sarifPath))) {
            errPrinter("Please provide a valid SARIF report path")
            return ERROR_EXIT
        }
        val sarifReport = try {
            SarifUtil.readReport(Paths.get(options.sarifPath))
        } catch (e: Exception) {
            errPrinter("Error reading SARIF report: ${e.message}")
            return ERROR_EXIT
        }

        val resolveInspectionName: (String) -> String = { id ->
            RuleUtil.findRuleDescriptor(sarifReport, id)?.shortDescription?.text ?: id
        }
        val printer = CommandLineResultsPrinter(simpleMemoize(resolveInspectionName), cliPrinter)
        return if (options.baselinePath != null) {
            compareBaselineThreshold(
                sarifReport,
                Paths.get(options.sarifPath),
                Paths.get(options.baselinePath),
                options.thresholds,
                options.includeAbsent,
                printer,
                cliPrinter,
                errPrinter
            )
        } else {
            compareThreshold(
                sarifReport,
                Paths.get(options.sarifPath),
                options.thresholds,
                printer,
                cliPrinter,
                errPrinter
            )
        }
    }

    private fun processResultCount(
        results: List<Result>?,
        hasBaseline: Boolean,
        thresholds: SeverityThresholds?,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Invocation {
        val size = results?.size ?: 0
        if (size > 0) {
            errPrinter("Found $size new problems according to the checks applied")
        } else {
            cliPrinter("It seems all right \uD83D\uDC4C No new problems found according to the checks applied")
        }
        val failedThresholds = thresholds?.let { checkSeverityThresholds(results, hasBaseline, it) }

        return if (failedThresholds.isNullOrEmpty()) {
            Invocation().apply {
                exitCode = 0
                executionSuccessful = true
            }
        } else {
            val msg = buildString {
                append(if (failedThresholds.size == 1) "Failure condition triggered" else "Failure conditions triggered")
                failedThresholds.joinTo(buffer = this, separator = "\n - ", prefix = "\n - " )
            }
            errPrinter(msg)
            return Invocation().apply {
                exitCode = THRESHOLD_EXIT
                exitCodeDescription = msg
                executionSuccessful = true
            }
        }
    }

    private fun compareThreshold(
        sarifReport: SarifReport,
        sarifPath: Path,
        thresholds: SeverityThresholds?,
        printer: CommandLineResultsPrinter,
        cliPrinter: (String) -> Unit,
        errPrinter: (String) -> Unit
    ): Int {
        val results = sarifReport.runs.first().results
        printer.printResults(results, "Qodana - Detailed summary")
        val invocation = processResultCount(results, false, thresholds, cliPrinter, errPrinter)
        sarifReport.runs.first().invocations = listOf(invocation)
        SarifUtil.writeReport(sarifPath, sarifReport)
        return invocation.exitCode
    }

    private fun compareBaselineThreshold(
        sarifReport: SarifReport,
        sarifPath: Path,
        baselinePath: Path,
        thresholds: SeverityThresholds?,
        includeAbsent: Boolean,
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
        BaselineCalculation.compare(sarifReport, baseline, BaselineCalculation.Options(includeAbsent))
        val results = sarifReport.runs.first().results
        printer.printResultsWithBaselineState(results, includeAbsent)
        val invocation = processResultCount(results, true, thresholds, cliPrinter, errPrinter)
        sarifReport.runs.first().invocations = listOf(invocation)
        SarifUtil.writeReport(sarifPath, sarifReport)
        return invocation.exitCode
    }

    private fun checkSeverityThresholds(
        results: List<Result>?,
        hasBaseline: Boolean,
        thresholds: SeverityThresholds
    ): List<String> {
        val baselineFilter: (Result) -> Boolean = when {
            !hasBaseline -> { _ -> true }
            else -> { x -> x.baselineState == Result.BaselineState.NEW }
        }

        val resultsBySeverity = results.orEmpty()
            .asSequence()
            .filter(baselineFilter)
            .groupingBy { it.severity() }
            .eachCount()

        val failedSeverities = resultsBySeverity.asSequence()
            .mapNotNull { (severity, count) ->
                val threshold = thresholds.bySeverity(severity)
                if (threshold != null && count > threshold) {
                    Triple(severity, count, threshold)
                } else {
                    null
                }
            }
            .sortedBy { (severity) -> severity }
            .map { (severity, count, threshold) ->
                val p = if (count == 1) "1 problem" else "$count problems"
                "Detected $p for severity ${severity.name}, fail threshold $threshold"
            }
            .toList()

        val total = resultsBySeverity.values.sum()
        return if (thresholds.any != null && total > thresholds.any) {
            val p = if (total == 1) "1 problem" else "$total problems"
            failedSeverities + "Detected $p across all severities, fail threshold ${thresholds.any}"
        } else {
            failedSeverities
        }
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
