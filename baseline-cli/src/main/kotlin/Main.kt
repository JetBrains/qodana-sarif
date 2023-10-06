import BaselineCli.process
import kotlin.system.exitProcess

const val THRESHOLD_EXIT = 255
const val ERROR_EXIT = 1

fun main(args: Array<String>) {
    val map = mutableMapOf<String, String>()

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-r" -> map["sarifReport"] = args.getOrNull(i + 1) ?: printUsageAndExit()
            "-b" -> map["baselineReport"] = args.getOrNull(i + 1) ?: printUsageAndExit()
            "-f" -> map["failThreshold"] = args.getOrNull(i + 1) ?: printUsageAndExit()
        }
        i += 2
    }

    if (!map.contains("sarifReport")) {
        printUsageAndExit()
    }

    val ret = process(map, { println(it) }, { System.err.println(it) })
    exitProcess(ret)
}

fun printUsageAndExit(): Nothing {
    println("Usage: -r <sarif_report> [-b <baseline_report>] [-f <fail_threshold>]")
    exitProcess(1)
}