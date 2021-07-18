package com.jetbrains.qodana.sarif.converter

import com.jetbrains.qodana.sarif.SarifUtil
import org.apache.logging.log4j.LogManager
import java.nio.file.Paths


class Main {
    companion object {
        private val log = LogManager.getLogger(Main::class.java)!!

        @JvmStatic
        fun main(args: Array<String>) {
            val applicationRunningParameters = ApplicationArgumentsHandler.handle(args) ?: return
            log.info("Starting exporting sarif...")


            val sarifPath = "/Users/nikita.kochetkov/qodana-sarif/Java_Junit5_Gradle_207_artifacts/qodana.sarif-Nikita.json"
            println("hello world")
            val sarif = SarifUtil.readReport(Paths.get(sarifPath))

            println("goodbye world")
        }
    }
}