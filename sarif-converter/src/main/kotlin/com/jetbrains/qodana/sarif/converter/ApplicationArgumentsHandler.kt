package com.jetbrains.qodana.sarif.converter

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.ParseException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.io.File

internal class ApplicationArgumentsHandler {
    companion object {
        private const val noVerboseConfigName = "log4j2-noVerbose.xml"
        private val log = LogManager.getLogger(ApplicationArgumentsHandler::class.java)!!


        fun handle(args: Array<String>): ApplicationRunningParameters? {
            val commandLine = parseArguments(args) ?: return null

            if (commandLine.hasOption("help")) {
                CLIProducer.printHelp()
                return null
            }

            if (!commandLine.validateArguments()) return null

            val applicationRunningParameters = ApplicationRunningParameters(commandLine)
            if (!applicationRunningParameters.verbose) {
                Configurator.reconfigure(Main::class.java.getResource("/${noVerboseConfigName}").toURI())
                log.trace("Loaded new log4j2 config $noVerboseConfigName")
            }

            return applicationRunningParameters
        }


        private fun parseArguments(args: Array<String>): CommandLine? {
            var commandLine: CommandLine? = null

            try {
                commandLine = CLIProducer.parser.parse(CLIProducer.options, args)
            } catch (exception: ParseException) {
                println(exception.message)
                CLIProducer.printHelp()
            }

            return commandLine
        }

        private fun CommandLine.validateArguments(): Boolean {
            if (hasOption("sp")) {
                val sarifFile = getOptionValue("sp")
                if (!File(sarifFile).exists()) {
                    log.error("Sarif file \"$sarifFile\" does not exist")
                    return false
                }
            } else {
                log.fatal("\"-sp\" parameters have to be specified, see --help")
                return false
            }

            return true
        }
    }
}