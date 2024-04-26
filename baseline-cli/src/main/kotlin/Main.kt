import BaselineCli.process
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlin.system.exitProcess

const val THRESHOLD_EXIT = 255
const val ERROR_EXIT = 1

class BaselineCommand : CliktCommand() {
    private val sarifReport: String by option("-r", help = "Sarif report path").required()
    private val baselineReport: String? by option("-b", help = "Baseline report path")
    private val baselineIncludeAbsent: Boolean by option("-i", help = "Baseline include absent status").flag()

    private fun threshold(severity: Severity): NullableOption<Int, Int> {
        val lc = severity.name.lowercase()
        return option("--threshold-$lc", help = "Fail threshold for $lc severity").int()
    }

    private val failThresholdAny: Int? by option(
        "-f",
        "--threshold-any",
        help = "Fail threshold for any severity"
    ).int()

    private val failThresholdCritical: Int? by threshold(Severity.CRITICAL)
    private val failThresholdHigh: Int? by threshold(Severity.HIGH)
    private val failThresholdModerate: Int? by threshold(Severity.MODERATE)
    private val failThresholdLow: Int? by threshold(Severity.LOW)
    private val failThresholdInfo: Int? by threshold(Severity.INFO)


    override fun run() {
        val thresholds = SeverityThresholds(
            any = failThresholdAny,
            critical = failThresholdCritical,
            high = failThresholdHigh,
            moderate = failThresholdModerate,
            low = failThresholdLow,
            info = failThresholdInfo
        )
        val ret = process(
            BaselineOptions(sarifReport, baselineReport, thresholds, baselineIncludeAbsent),
            ::println,
            System.err::println
        )
        exitProcess(ret)
    }
}

internal data class BaselineOptions(
    val sarifPath: String,
    val baselinePath: String? = null,
    val thresholds: SeverityThresholds? = null,
    val includeAbsent: Boolean = false,
)

fun main(args: Array<String>) = BaselineCommand().main(args)
