import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

class BaselineCliTest {

    private val sarif = run {
        val tmp = Files.createTempFile(null, ".sarif")
        Files.copy(Path("src/test/resources/report.equal.sarif.json"), tmp, StandardCopyOption.REPLACE_EXISTING)
        tmp.toFile().also(File::deleteOnExit).absolutePath
    }

    private val stdout = StringBuilder()
    private val stderr = StringBuilder()

    private fun doProcess(reportPath: String, baselinePath: String? = null, failThreshold: Int? = null) =
        BaselineCli.process(
            Path(reportPath),
            baselinePath?.let(::Path),
            failThreshold,
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
            doProcess(sarif, "nonExistentBaselineReport.sarif")
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide valid baseline report path", stderr.toString())
    }

    @Test
    fun `test when there is a error reading baselineReport`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(sarif, "src/test/resources/corrupted.sarif.json")
        }

        // Assert
        assertTrue(stderr.startsWith("Error reading baseline report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when failThreshold is not present and results count is less than the failThreshold default value`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(sarif)
        }

        // Assert
        assertEquals(0, exitCode)
        assertTrue(!stdout.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is provided and results count in sarifReport is more than failThreshold`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(sarif, failThreshold = 0)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(stderr.contains("New problems count") && stderr.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is provided and newResults count in baselineCalculation is more than failThreshold`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(sarif, "src/test/resources/empty.sarif.json", 1)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(stderr.contains("New problems count") && stderr.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is not provided or is less than newResults count in baselineCalculation`() {
        // Act
        val exitCode = assertDoesNotThrow {
            doProcess(sarif, "src/test/resources/empty.sarif.json")
        }

        // Assert
        assertEquals(0, exitCode) // if the failThreshold is not provided, the function should not return a failure
        assertFalse(stdout.contains("New problems count") && stdout.contains("is greater than the threshold"))
    }

    @Test
    fun `test when rule description is available`() {
        // Act
        assertDoesNotThrow { doProcess("src/test/resources/report.with-description.sarif.json") }

        // Assert
        assertTrue(stdout.contains("Result of method call ignored")) // the unresolved ID
        assertFalse(stdout.contains("ResultOfMethodCallIgnored")) // the unresolved ID
    }

}
