import BaselineCli.process
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

const val THRESHOLD_EXIT = 255
const val ERROR_EXIT = 1

internal object BaselineCommand: CliktCommand() {
    private val report: Path by option("-r", "--report")
        .path(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true)
        .required()
        .help("Sarif report to compare against the baseline")

    private val baseline: Path? by option("-b", "--baseline")
        .path(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true)
        .help("Baseline to compare the report against")

    private val failThreshold: Int? by option("-f", "--fail-threshold")
        .int()
        .help("Exclusive number of results at which the run is considered a failure")

    override fun run() {
        val code = process(report, baseline, failThreshold, ::println, System.err::println)
        if (code != 0) throw CliktError(printError = false, statusCode = code)
    }
}
fun main(args: Array<String>) = BaselineCommand.main(args)
