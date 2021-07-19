package com.jetbrains.qodana.sarif.app

import com.jetbrains.qodana.sarif.SarifConverter
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
                    SarifConverter(sarifFile).convert(output)
                }
                log.info("Done")
            }.onFailure { e ->
                log.error("Problems occurred while trying covert sarif file")
                log.error(e.message)
            }
        }
    }
}