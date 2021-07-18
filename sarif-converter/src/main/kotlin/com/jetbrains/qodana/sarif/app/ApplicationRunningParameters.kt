package com.jetbrains.qodana.sarif.app

import org.apache.commons.cli.CommandLine
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

internal class ApplicationRunningParameters(commandLine: CommandLine) {
    val sarifFile: File = File(commandLine.getOptionValue("sp"))
    val verbose: Boolean = commandLine.hasOption("vb")
    val output: Path = if (commandLine.hasOption("o")) {
        Paths.get(commandLine.getOptionValue("o"))
    } else {
        Paths.get(".")
    }
}