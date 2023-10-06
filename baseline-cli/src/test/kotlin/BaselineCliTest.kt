import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.readText

class BaselineCliTest {
    @Test
    fun `test when sarifReport does not exist in the provided path`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = "nonExistentPath.sarif"
        }
        var printedMessage: String? = null
        val testCliPrinter: (String) -> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide a valid SARIF report path", printedMessage)
    }

    @Test
    fun `test when there is an error reading sarifReport`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply{
            this["sarifReport"] = Path("src/test/resources/corrupted.sarif.json").toString()
        }
        var printedMessage: String? = null
        val testCliPrinter: (String) -> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertNotNull(printedMessage)
        assertTrue(printedMessage!!.startsWith("Error reading SARIF report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when baselineReport path is provided and file does not exist`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] =  Path("src/test/resources/report.equal.sarif.json").toString()
            this["baselineReport"] = "nonExistentBaselineReport.sarif"
        }
        var printedMessage: String? = null
        val testCliPrinter: (String) -> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertEquals(ERROR_EXIT, exitCode)
        assertEquals("Please provide valid baseline report path", printedMessage)
    }

    @Test
    fun `test when there is a error reading baselineReport`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply{
            this["sarifReport"] = Path("src/test/resources/report.equal.sarif.json").toString()
            this["baselineReport"] = Path("src/test/resources/corrupted.sarif.json").toString()
        }
        var printedMessage: String? = null
        val testCliPrinter: (String) -> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertNotNull(printedMessage)
        assertTrue(printedMessage!!.startsWith("Error reading baseline report"))
        assertEquals(ERROR_EXIT, exitCode)
    }

    @Test
    fun `test when failThreshold is not present and results count is less than the failThreshold default value`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = Path("src/test/resources/report.equal.sarif.json").toString()
        }
        var printedMessage: String? = null
        val testCliPrinter: (String) -> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertEquals(0, exitCode)
        assertTrue(printedMessage == null || !printedMessage!!.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is provided and results count in sarifReport is more than failThreshold`() {
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = Path("src/test/resources/report.equal.sarif.json").toString()
            this["failThreshold"] = "0"
        }
        var printedMessage: String? = null
        val testCliPrinter: (String) -> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(printedMessage!!.contains("New problems count") && printedMessage!!.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is provided and newResults count in baselineCalculation is more than failThreshold`() {
        val tempFile = File.createTempFile("temp", ".sarif")
        tempFile.writeText(Path("src/test/resources/report.equal.sarif.json").readText())
        tempFile.deleteOnExit()
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = tempFile.absolutePath.toString()
            this["baselineReport"] = Path("src/test/resources/empty.sarif.json").toString()
            this["failThreshold"] = "1"
        }
        var printedMessage: String? = null
        val testCliPrinter: (String) -> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertEquals(THRESHOLD_EXIT, exitCode)
        assertTrue(printedMessage!!.contains("New problems count") && printedMessage!!.contains("is greater than the threshold"))
    }

    @Test
    fun `test when failThreshold is not provided or is less than newResults count in baselineCalculation`() {
        val tempFile = File.createTempFile("temp", ".sarif")
        tempFile.writeText(Path("src/test/resources/report.equal.sarif.json").readText())
        tempFile.deleteOnExit()
        // Arrange
        val map = mutableMapOf<String, String>().apply {
            this["sarifReport"] = tempFile.absolutePath.toString()
            this["baselineReport"] = Path("src/test/resources/empty.sarif.json").toString()
        }
        var printedMessage: String? = null
        val testCliPrinter: (String)-> Unit = {
            printedMessage = it
        }

        // Act
        val exitCode = assertDoesNotThrow {
            BaselineCli.process(map, testCliPrinter)
        }

        // Assert
        assertEquals(0, exitCode) // if the failThreshold is not provided, the function should not return a failure
        assertFalse(printedMessage!!.contains("New problems count") && printedMessage!!.contains("is greater than the threshold"))
    }
}