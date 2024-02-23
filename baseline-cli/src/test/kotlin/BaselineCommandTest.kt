import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BaselineCommandTest {
    @Test
    fun `report option should be required`() {
        val e = evaluateArguments()
        assertEquals(1, e.statusCode)
        assertContains("missing option --report", e.stderr)
    }

    @Test
    fun `report file must exist`() {
        val e = evaluateArguments("--report" to "/this/does/not/exist")
        assertEquals(1, e.statusCode)
        assertContains("""file "/this/does/not/exist" does not exist.""", e.stderr)
    }

    @Test
    fun `baseline must exist if given`() {
        val e = evaluateArguments(
            "--baseline" to "/this/baseline/does/not/exist",
            "--report" to DEFAULT_REPORT
        )
        assertEquals(1, e.statusCode)
        assertContains("""file "/this/baseline/does/not/exist" does not exist.""", e.stderr)
    }

    @Test
    fun `scope path must exist if given`() {
        val e = evaluateArguments(
            "--scope-path" to "/this/scope/does/not/exist",
            "--report" to DEFAULT_REPORT
        )
        assertEquals(1, e.statusCode)
        assertContains("""file "/this/scope/does/not/exist" does not exist.""", e.stderr)
    }

    @Test
    fun `scope path requires baseline path if given`() {
        val e = evaluateArguments(
            "--scope-path" to tempPath("scope").toString(),
            "--report" to DEFAULT_REPORT
        )
        assertEquals(1, e.statusCode)
        assertContains("missing option --baseline", e.stderr)
    }

    @Test
    fun `fail threshold must not be negative if given`() {
        val e = evaluateArguments(
            "--report" to DEFAULT_REPORT,
            "--fail-threshold" to "-1"
        )
        assertEquals(1, e.statusCode)
        assertContains("invalid value for --fail-threshold: Must not be negative", e.stderr)
    }

    private fun evaluateArguments(vararg args: Pair<String, String>) =
        BaselineCommand.test(args.toList().map { (k, v) -> "$k=$v" } + "--test-only")

}
