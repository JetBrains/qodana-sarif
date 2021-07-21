package com.jetbrains.qodana.sarif

import com.jetbrains.qodana.sarif.Util.Companion.getResourceFile
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

internal class SarifConverterImplTest {
//    Need fix charOffset, charLength, + type of source
//    @Test
    fun `have to convert sarif to expected UI files`() {
        val sarifFile = getResourceFile("sarif/qodana.sarif.json")
        val expectedMetaInformation = getResourceFile("sarif/expected/metaInformation.json")
        val expectedResultsAllProblems = getResourceFile("sarif/expected/result-allProblems.json")
        val tempDirectory = Paths.get("tempTestDirectory").apply { toFile().mkdirs() }
        println("dir path: ${tempDirectory.toAbsolutePath()}")

        try {
            SarifConverterImpl().convert(sarifFile, tempDirectory)
            Assert.assertEquals(expectedMetaInformation.readText(), tempDirectory.resolve("metaInformation.json").toUri().toURL().readText())
            Assert.assertEquals(expectedResultsAllProblems.readText(), tempDirectory.resolve("result-allProblems.json").toUri().toURL().readText())
        } finally {
            tempDirectory.toFile().deleteRecursively()
        }
    }
}