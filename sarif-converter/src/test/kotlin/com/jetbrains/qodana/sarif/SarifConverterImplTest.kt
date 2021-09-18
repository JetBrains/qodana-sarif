package com.jetbrains.qodana.sarif

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.jetbrains.qodana.sarif.Util.Companion.getResourceFile
import com.jetbrains.qodana.sarif.Util.Companion.readFileAsText
import com.jetbrains.qodana.sarif.Util.Companion.readText
import org.jetbrains.teamcity.qodana.json.version3.SimpleProblem
import org.junit.Assert
import org.junit.Test
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
    
    @Test
    fun `have to convert baseline sarif report to expected UI files`() {
        assertConverterWorksOn("sarif/baseline-sarif")
    }

    internal data class ResultAllProblems(val version: String = "3", @SerializedName("listProblem") val problems: List<SimpleProblem> = emptyList()) {
        fun mergeHashes(src: ResultAllProblems): ResultAllProblems {
            Assert.assertEquals(src.problems.size, problems.size)
            return copy(problems = src.problems.zip(problems).map { (srcProblem, dstProblem) -> dstProblem.copy(hash = srcProblem.hash) })
        }
    }

    private fun assertResultsEqualExceptHash(actual: String, expected: String) {
        val srcObj = gson.fromJson(actual, ResultAllProblems::class.java)
        val dstObj = gson.fromJson(expected, ResultAllProblems::class.java).mergeHashes(srcObj)
        Assert.assertEquals(gson.toJson(srcObj), gson.toJson(dstObj))
    }


    private fun assertConverterWorksOn(dirPath: String) {
        val sarifFile = getResourceFile("$dirPath/qodana.sarif.json")
        val expectedMetaInformation = readFileAsText("/$dirPath/expected/metaInformation.json")
        val expectedResultsAllProblems = readFileAsText("/$dirPath/expected/result-allProblems.json")
        val tempDirectory = Paths.get("tempTestDirectory_1").apply { toFile().mkdirs() }

        try {
            SarifConverterImpl().convert(sarifFile, tempDirectory)
            val actualResultsAllProblems = tempDirectory.resolve("result-allProblems.json").readText()
            val actualMetaInformation = tempDirectory.resolve("metaInformation.json").readText()
            assertResultsEqualExceptHash(actualResultsAllProblems, expectedResultsAllProblems)
            Assert.assertEquals(expectedMetaInformation, actualMetaInformation)
        } finally {
            tempDirectory.toFile().deleteRecursively()
        }
    }

    companion object {
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }
}