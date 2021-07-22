package com.jetbrains.qodana.sarif

import com.jetbrains.qodana.sarif.Util.Companion.getResourceFile
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

internal class SarifConverterImplTest {
    @Test
    fun `have to convert Qodana IntelliJ Linter sarif to expected UI files`() {
        val sarifFile = getResourceFile("sarif/intellij/qodana.sarif.json")
        val expectedMetaInformation = getResourceFile("sarif/intellij/expected/metaInformation.json")
        val expectedResultsAllProblems = getResourceFile("sarif/intellij/expected/result-allProblems.json")
        val tempDirectory = Paths.get("tempTestDirectory_1").apply { toFile().mkdirs() }

        try {
            SarifConverterImpl().convert(sarifFile, tempDirectory)
            Assert.assertEquals(expectedMetaInformation.readText(), tempDirectory.resolve("metaInformation.json").toUri().toURL().readText())
            Assert.assertEquals(expectedResultsAllProblems.readText(), tempDirectory.resolve("result-allProblems.json").toUri().toURL().readText())
        } finally {
//            tempDirectory.toFile().deleteRecursively()
        }
    }
}