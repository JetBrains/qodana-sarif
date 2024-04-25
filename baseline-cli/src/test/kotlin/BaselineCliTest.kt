import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

class BaselineCliTest {

    private val sarif = copySarifFromResources("report.equal.sarif.json")
    private val emptySarif = copySarifFromResources("empty.sarif.json")
    private val corruptedSarif = Path("src/test/resources/corrupted.sarif.json").toString()

    private val stdout = StringBuilder()
    private val stderr = StringBuilder()

    @Test
    fun `test when sarifReport does not exist in the provided path`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions("nonExistentPath.sarif"), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide a valid SARIF report path", stderr.toString())
    }

    @Test
    fun `test when there is an error reading sarifReport`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(corruptedSarif), stdout::append, stderr::append)
        }

        // Assert
        assertTrue(stderr.startsWith("Error reading SARIF report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when baselineReport path is provided and file does not exist`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, "nonExistentBaselineReport.sarif"), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide valid baseline report path", stderr.toString())
    }

    @Test
    fun `test when there is a error reading baselineReport`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, corruptedSarif), stdout::append, stderr::append)
        }

        // Assert
        assertTrue(stderr.startsWith("Error reading baseline report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when failThreshold is provided and results count in sarifReport is more than failThreshold`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, thresholds = SeverityThresholds(any = 0)), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(stderr.contains("Detected 2 problems across all severities, fail threshold 0"))
    }

    @Test
    fun `test when failThreshold is provided and results count in sarifReport is equal to failThreshold`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, thresholds = SeverityThresholds(any = 2)), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(0, exitCode)
        assertTrue(stderr.contains("Found 2 new problems according to the checks applied"))
    }

    @Test
    fun `test when fail threshold for severity is provided and results count exceeds threshold`() {
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, thresholds = SeverityThresholds(high = 1)), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(stderr.contains("Detected 2 problems for severity HIGH, fail threshold 1"))
    }

    @Test
    fun `test when fail threshold for severity is provided and results count does not exceed threshold`() {
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, thresholds = SeverityThresholds(low = 2)), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(0, exitCode)
        assertFalse(stderr.contains("LOW"))
        assertTrue(stderr.contains("Found 2 new problems according to the checks applied"))
    }

    @Test
    fun `test when failThreshold is provided and newResults count in baselineCalculation is more than failThreshold`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, emptySarif, thresholds = SeverityThresholds(any = 1)), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(stderr.contains("Detected 2 problems across all severities, fail threshold 1"))
    }

    @Test
    fun `test when failThreshold is not provided or is less than newResults count in baselineCalculation`() {
        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(BaselineOptions(sarif, emptySarif), stdout::append, stderr::append)
        }

        // Assert
        assertEquals(0, exitCode) // if the failThreshold is not provided, the function should not return a failure
        assertFalse(stdout.contains("New problems count") && stdout.contains("is greater than the threshold"))
    }

    @Test
    fun `test when rule description is available`() {
        // Act
        assertDoesNotThrow { BaselineCli.process(BaselineOptions(Path("src/test/resources/report.with-description.sarif.json").toString()), stdout::append, stderr::append) }

        // Assert
        assertTrue(stdout.contains("Result of method call ignored")) // the unresolved ID
        assertFalse(stdout.contains("ResultOfMethodCallIgnored")) // the unresolved ID
    }

    @Test
    fun `test include absent true`() {
        // Act
        assertDoesNotThrow { BaselineCli.process(BaselineOptions(emptySarif, sarif, includeAbsent = true), stdout::append, stderr::append) }

        // Assert
        assertTrue(stdout.contains("ABSENT: 2"))
        val content = File(emptySarif).readText(charset("UTF-8"))
        assertTrue(content.contains("absent"))
    }

    @Test
    fun `test include absent false`() {
        // Act
        assertDoesNotThrow { BaselineCli.process(BaselineOptions(copySarifFromResources("single.sarif.json"), sarif, includeAbsent = false), stdout::append, stderr::append) }

        // Assert
        assertFalse(stdout.contains("ABSENT:"))
        assertTrue(stdout.contains("UNCHANGED: 1"))
        val content = File(emptySarif).readText(charset("UTF-8"))
        assertFalse(content.contains("absent"))
    }


    private fun copySarifFromResources(name: String) = run {
        val tmp = Files.createTempFile(null, ".sarif")
        Files.copy(Path("src/test/resources/$name"), tmp, StandardCopyOption.REPLACE_EXISTING)
        tmp.toFile().also(File::deleteOnExit).absolutePath
    }
}
