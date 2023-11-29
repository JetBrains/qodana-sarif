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

    @Test
    fun `test when sarifReport does not exist in the provided path`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = "nonExistentPath.sarif"
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide a valid SARIF report path", stderr.toString())
    }

    @Test
    fun `test when there is an error reading sarifReport`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = Path("src/test/resources/corrupted.sarif.json").toString()
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertTrue(stderr.startsWith("Error reading SARIF report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when baselineReport path is provided and file does not exist`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = sarif
            this["baselineReport"] = "nonExistentBaselineReport.sarif"
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide valid baseline report path", stderr.toString())
    }

    @Test
    fun `test when there is a error reading baselineReport`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = sarif
            this["baselineReport"] = Path("src/test/resources/corrupted.sarif.json").toString()
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertTrue(stderr.startsWith("Error reading baseline report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when failThreshold is not present and results count is less than the failThreshold default value`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = sarif
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertEquals(0, exitCode)
        assertTrue(!stdout.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is provided and results count in sarifReport is more than failThreshold`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = sarif
            this["failThreshold"] = "0"
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(stderr.contains("New problems count") && stderr.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is provided and newResults count in baselineCalculation is more than failThreshold`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = sarif
            this["baselineReport"] = Path("src/test/resources/empty.sarif.json").toString()
            this["failThreshold"] = "1"
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(stderr.contains("New problems count") && stderr.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is not provided or is less than newResults count in baselineCalculation`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = sarif
            this["baselineReport"] = Path("src/test/resources/empty.sarif.json").toString()
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, stdout::append, stderr::append)
        }

        // Assert
        assertEquals(0, exitCode) // if the failThreshold is not provided, the function should not return a failure
        assertFalse(stdout.contains("New problems count") && stdout.contains("is greater than the threshold"))
    }

    @Test
    fun `test when rule description is available`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = Path("src/test/resources/report.with-description.sarif.json").toString()
        }

        // Act
        assertDoesNotThrow { BaselineCli.process(map, stdout::append, stderr::append) }

        // Assert
        assertTrue(stdout.contains("Result of method call ignored")) // the unresolved ID
        assertFalse(stdout.contains("ResultOfMethodCallIgnored")) // the unresolved ID
    }

}
