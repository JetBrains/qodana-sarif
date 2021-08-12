package com.jetbrains.qodana.sarif

import com.google.gson.GsonBuilder
import com.jetbrains.qodana.sarif.Util.Companion.getResourceFile
import com.jetbrains.qodana.sarif.model.MetaInformation
import org.jetbrains.teamcity.qodana.json.version3.Problem
import org.jetbrains.teamcity.qodana.json.version3.SimpleProblem
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

internal class SarifConverterImplTest {
    @Test
    fun `have to convert Qodana IntelliJ Linter sarif to expected UI files`() {
        assertConverterWorksOn("sarif/intellij")
    }

    @Test
    fun `have to convert Marketplace Linter sarif output to expected UI files`() {
        assertConverterWorksOn("sarif/marketplace")
    }

    @Test
    fun `have to convert Checkov Linter sarif output to expected UI files`() {
        assertConverterWorksOn("sarif/checkov")
    }

    companion object {
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }

    private inline fun <reified T> readResourceClass(path: String): T {
        return gson.fromJson(getResourceFile(path).readText(), T::class.java)
    }

    private inline fun <reified T> readPathClass(path: Path): T {
        return gson.fromJson(path.toUri().toURL().readText(), T::class.java)
    }

    internal data class ResultAllProblems(val version: String = "3", val problems: List<SimpleProblem> = emptyList())

    private fun assertConverterWorksOn(dirPath: String) {
        val sarifFile = getResourceFile("$dirPath/qodana.sarif.json")
        val expectedMetaInformation = readResourceClass<MetaInformation>("$dirPath/expected/metaInformation.json")
        val expectedResultsAllProblems = readResourceClass<ResultAllProblems>("$dirPath/expected/result-allProblems.json")
        val tempDirectory = Paths.get("tempTestDirectory_1").apply { toFile().mkdirs() }

        try {
            SarifConverterImpl().convert(sarifFile, tempDirectory)
            val actualResultsAllProblems = readPathClass<ResultAllProblems>(tempDirectory.resolve("result-allProblems.json"))
            val actualMetaInformation = readPathClass<MetaInformation>(tempDirectory.resolve("metaInformation.json"))
            Assert.assertEquals(expectedResultsAllProblems, actualResultsAllProblems)
            Assert.assertEquals(expectedMetaInformation, actualMetaInformation)
        } finally {
            tempDirectory.toFile().deleteRecursively()
        }
    }

    fun SimpleProblem.equals(other: Any?): Boolean {
        // Don't use hash for comparison, because some sarifs won't have it
        return when (other) {
            is Problem -> {
                return (this.attributes == other.attributes) &&
                        (this.category == other.category) &&
                        (this.comment == other.comment) &&
                        (this.detailsInfo == other.detailsInfo) &&
                        (this.severity == other.severity) &&
                        (this.sources == other.sources) &&
                        (this.tags == other.tags) &&
                        (this.tool == other.tool) &&
                        (this.type == other.type)
            }
            else -> super.equals(other)
        }
    }
}