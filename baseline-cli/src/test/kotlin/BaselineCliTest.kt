import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Path
import kotlin.io.path.writeText

class BaselineCliTest {

    private val stdout = StringBuilder()
    private val stderr = StringBuilder()

    private fun doProcess(
        reportPath: String,
        baselinePath: String? = null,
        failThreshold: Int? = null,
        scopePath: Path? = null
    ) = BaselineCli.process(
        // create copies because we might mutate the files
        copyOf(reportPath),
        baselinePath?.let(::copyOf),
        failThreshold,
        scopePath,
        stdout::append,
        stderr::append
    )

    @Test
    fun `test when sarifReport does not exist in the provided path`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess("nonExistentPath.sarif")
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide a valid SARIF report path", stderr.toString())
    }

    @Test
    fun `test when there is an error reading sarifReport`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess("src/test/resources/corrupted.sarif.json")
        }

        // Assert
        assertTrue(stderr.startsWith("Error reading SARIF report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when baselineReport path is provided and file does not exist`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(DEFAULT_REPORT, "nonExistentBaselineReport.sarif")
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide valid baseline report path", stderr.toString())
    }

    @Test
    fun `test when there is a error reading baselineReport`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(DEFAULT_REPORT, "src/test/resources/corrupted.sarif.json")
        }

        // Assert
        assertTrue(stderr.startsWith("Error reading baseline report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when failThreshold is not present and results count is less than the failThreshold default value`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(DEFAULT_REPORT)
        }

        // Assert
        assertEquals(0, exitCode)
        assertNotContains("is greater than the threshold", stdout)
    }

    @Test
    fun `test when failThreshold is provided and results count in sarifReport is more than failThreshold`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(DEFAULT_REPORT, failThreshold = 0)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertContains("New problems count", stderr)
        assertContains("is greater than the threshold", stderr)
    }

    @Test
    fun `test when failThreshold is provided and newResults count in baselineCalculation is more than failThreshold`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(DEFAULT_REPORT, "src/test/resources/empty.sarif.json", 1)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertContains("New problems count", stderr)
        assertContains("is greater than the threshold", stderr)
    }

    @Test
    fun `test when failThreshold is not provided or is less than newResults count in baselineCalculation`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(DEFAULT_REPORT, "src/test/resources/empty.sarif.json")
        }

        // Assert
        assertEquals(0, exitCode) // if the failThreshold is not provided, the function should not return a failure
        assertNotContains("New problems count", stdout)
        assertNotContains("is greater than the threshold", stdout)
    }

    @Test
    fun `test when rule description is available`() {
        // Act
        assertDoesNotThrow { doProcess("src/test/resources/report.with-description.sarif.json") }

        // Assert
        assertContains("Result of method call ignored", stdout) // the unresolved ID
        assertNotContains("ResultOfMethodCallIgnored", stdout) // the unresolved ID
    }

    @Test
    @Disabled("Need to generate test data first")
    fun `should use scope file if given`() {
        val scope = tempPath("scope")
        scope.writeText("test-module/A.java")

        // Act
        doProcess(
            reportPath = DEFAULT_REPORT,
            baselinePath = "src/test/resources/baseline.sarif.json",
            scopePath = scope
        )

        // Assert
        assertContains("Found 1 new problems according to the checks applied", stdout)
    }

}
