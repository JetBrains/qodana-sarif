package com.jetbrains.qodana.sarif.converter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

internal object CLIProducer {
    val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    val options = createOptions()
    val parser = DefaultParser()
    private val helper = HelpFormatter()

    fun printHelp() {
        helper.printHelp(
            100,
            "java -jar jarName",
            "Standard commands:",
            options,
            "Please report issues at https://youtrack.jetbrains.com/issues/QD",
            true
        )
    }

    private fun createOptions(): Options {
        val sarifPath = Option("sp", "sarif-path", true, "Path to SARIF file").apply { isRequired = true }
        val output = Option("o", "output", true, "Output directory for report files")
        val verbose = Option("vb", "verbose", false, "Enable verbose logging")
        val help = Option("help","help", false, "Print program options")

        return Options().addOption(sarifPath).addOption(output).addOption(verbose).addOption(help)
    }
}