package com.jetbrains.qodana.sarif.app

import com.jetbrains.qodana.sarif.SarifConverterImpl
import org.apache.logging.log4j.LogManager


class Main {
    companion object {
        private val log = LogManager.getLogger(Main::class.java)!!

        @JvmStatic
        fun main(args: Array<String>) {
            val applicationRunningParameters = ApplicationArgumentsHandler.handle(args) ?: return
            log.info("Starting exporting sarif")

            runCatching {
                applicationRunningParameters.run {
                    SarifConverterImpl().convert(sarifFile, output)
                }
                log.info("Done")
            }.onFailure { ex ->
                log.error("Problems occurred while trying covert sarif file")
                log.error(ex)
            }
        }
    }
}