import BaselineCli.process
import com.github.ajalt.clikt.core.CliktCommand
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
    private val failThreshold: Int? by option("-f", help = "Fail threshold").int()
    override fun run() {
        val ret = process(
            BaselineOptions(sarifReport, baselineReport, failThreshold, baselineIncludeAbsent),
            { println(it) },
            { System.err.println(it) })
        exitProcess(ret)
    }
}

data class BaselineOptions(val sarifPath: String,
                           val baselinePath: String? = null,
                           val failThreshold: Int? = null,
                           val includeAbsent: Boolean = false)

fun main(args: Array<String>) = BaselineCommand().main(args)
