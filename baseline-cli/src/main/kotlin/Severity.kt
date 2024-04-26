import com.jetbrains.qodana.sarif.model.Level
import com.jetbrains.qodana.sarif.model.Result


internal data class SeverityThresholds(
    val any: Int? = null,
    val critical: Int? = null,
    val high: Int? = null,
    val moderate: Int? = null,
    val low: Int? = null,
    val info: Int? = null,
) {
    fun bySeverity(qodanaSeverity: Severity) = when (qodanaSeverity) {
        Severity.INFO -> info
        Severity.LOW -> low
        Severity.MODERATE -> moderate
        Severity.HIGH -> high
        Severity.CRITICAL -> critical
    }
}

internal enum class Severity {
    CRITICAL,
    HIGH,
    MODERATE,
    LOW,
    INFO;

    companion object {
        fun Result.severity() =
            (properties?.get("qodanaSeverity") as? String)?.let(::fromQodanaSeverity)
                ?: (properties?.get("ideaSeverity") as? String)?.let(::fromIdeaSeverity)
                ?: level?.let(::fromSarif)
                ?: MODERATE

        private fun fromQodanaSeverity(value: String): Severity? = when (value) {
            "Critical" -> CRITICAL
            "High" -> HIGH
            "Moderate" -> MODERATE
            "Low" -> LOW
            "Info" -> INFO
            else -> null
        }

        private fun fromIdeaSeverity(value: String): Severity = when (value) {
            "ERROR" -> CRITICAL
            "WARNING" -> HIGH
            "WEAK_WARNING" -> MODERATE
            "TYPO" -> LOW
            else -> INFO
        }

        private fun fromSarif(level: Level): Severity = when (level) {
            Level.ERROR -> CRITICAL
            Level.WARNING -> HIGH
            else -> MODERATE
        }
    }

}
