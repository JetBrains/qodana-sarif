import org.junit.jupiter.api.Assertions
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.notExists

internal const val DEFAULT_REPORT = "src/test/resources/report.equal.sarif.json"

internal fun copyOf(path: String): Path {
    val src = Path(path)
    if (src.notExists()) return src

    val tmp = tempPath(src.name)
    Files.copy(src, tmp, StandardCopyOption.REPLACE_EXISTING)
    return tmp
}

internal fun tempPath(name: String) =
    Files.createTempFile(name, null).also { it.toFile().deleteOnExit() }

internal fun assertContains(expect: String, actual: CharSequence) {
    Assertions.assertTrue(expect in actual, "expected '$actual' to contain '$expect'")
}

internal fun assertNotContains(expect: String, actual: CharSequence) {
    Assertions.assertFalse(expect in actual, "expected '$actual' to NOT contain '$expect'")
}
