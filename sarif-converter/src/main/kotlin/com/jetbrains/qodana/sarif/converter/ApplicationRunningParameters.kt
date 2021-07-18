package com.jetbrains.qodana.sarif.converter

import org.apache.commons.cli.CommandLine
import java.io.File
import java.nio.file.Paths

internal class ApplicationRunningParameters(commandLine: CommandLine) {
    val sarifFile: File = File(commandLine.getOptionValue("sp"))
    val verbose: Boolean = commandLine.hasOption("vb")
    val output: File = if (commandLine.hasOption("o")) {
        File(commandLine.getOptionValue("o"))
    } else {
        Paths.get(".").toFile()
    }
}
