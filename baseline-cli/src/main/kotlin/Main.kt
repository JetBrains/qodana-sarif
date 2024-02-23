import BaselineCli.process
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

const val THRESHOLD_EXIT = 255
const val ERROR_EXIT = 1

internal object BaselineCommand: CliktCommand() {
    private class CompareGroup : OptionGroup(name = "Baseline Comparison Options") {
        val baseline: Path by option("-b", "--baseline")
            .path(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true)
            .help("Baseline to compare the report against")
            .required()

        val scopePath: Path? by option("-s", "--scope-path")
            .path(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true)
            .help("A file describing the scope of this, i.e. created by 'git diff --name-only'".trimIndent())
    }

    private val report: Path by option("-r", "--report")
        .path(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true)
        .required()
        .help("Sarif report to compare against the baseline")

    private val failThreshold: Int? by option("-f", "--fail-threshold")
        .int()
        .help("Exclusive number of results at which the run is considered a failure")
        .check("Must not be negative") { it >= 0 }

    private val comparison by CompareGroup().cooccurring()

    private val testOnly by option("--test-only", hidden = true)
        .flag()

    override fun run() {
        // only evaluating options is not supported https://github.com/ajalt/clikt/issues/489
        if (testOnly) return
        val code = process(
            reportPath = report,
            baselinePath = comparison?.baseline,
            failThreshold = failThreshold,
            scopePath = comparison?.scopePath,
            cliPrinter = ::println,
            errPrinter = System.err::println
        )
        if (code != 0) throw CliktError(printError = false, statusCode = code)
    }
}
fun main(args: Array<String>) = BaselineCommand.main(args)
