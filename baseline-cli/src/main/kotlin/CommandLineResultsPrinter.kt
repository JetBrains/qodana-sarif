import com.jetbrains.qodana.sarif.model.Level
import com.jetbrains.qodana.sarif.model.Result

class CommandLineResultsPrinter(
    private val inspectionIdToName: (inspectionId: String) -> String,
    private val cliPrinter: (result: String) -> Unit,
) {
    fun printResultsWithBaselineState(results: List<Result>, includeAbsent: Boolean) {
        val resultsCountByBaselineState = results.groupingBy { it.baselineState }.eachCount()

        val unchanged = resultsCountByBaselineState[Result.BaselineState.UNCHANGED] ?: 0
        val new = resultsCountByBaselineState[Result.BaselineState.NEW] ?: 0
        val absent = resultsCountByBaselineState[Result.BaselineState.ABSENT] ?: 0
        val groupingMessage = "Grouping problems according to baseline: UNCHANGED: $unchanged, NEW: $new, ABSENT: $absent"

        val countedProblems = (
                results
                    .takeIf { !includeAbsent }?.filter { it.baselineState != Result.BaselineState.ABSENT }
                    ?: results)
            .groupingBy {
                Triple(
                    inspectionIdToName.invoke(it.ruleId),
                    it.baselineState,
                    it.level
                )
            }.eachCount()

        printProblemsCountTable(
            "Qodana - Baseline summary",
            groupingMessage,
            listOf(
                "Name",
                "Baseline",
                "Severity"
            ),
            listOf(50, 0, 0),
            countedProblems,
            compareByDescending<Map.Entry<Triple<String, Result.BaselineState, Level>, Int>> { it.key.second.order }
                .thenByDescending { it.key.third }.thenByDescending { it.value }.thenBy { it.key.first }
        ) {
            listOf(it.first, it.second.value().uppercase(), it.third.toString())
        }
    }

    private val Result.BaselineState.order: Int
        get() = when (this) {
            Result.BaselineState.UNCHANGED -> 0
            Result.BaselineState.UPDATED -> 1
            Result.BaselineState.ABSENT -> 2
            Result.BaselineState.NEW -> 3
        }

    fun printResults(results: List<Result>, sectionTitle: String, message: String? = null) {
        val countedByLevels = results.groupingBy { it.level }.eachCount().toSortedMap(compareByDescending { it })
        val groupingMessage = message ?: "By severity: ${countedByLevels.map { "${it.key} - ${it.value}" }.joinToString(", ")}"

        val countedProblems = results.groupingBy { inspectionIdToName(it.ruleId) to it.level }.eachCount()

        printProblemsCountTable(
            sectionTitle,
            groupingMessage,
            listOf(
                "Name",
                "Severity"
            ),
            listOf(50, 0),
            countedProblems,
            compareByDescending<Map.Entry<Pair<String, Level>, Int>> { it.key.second }
                .thenByDescending { it.value }.thenBy { it.key.first }
        ) {
            listOf(it.first, it.second.toString())
        }
    }

    private fun <T> printProblemsCountTable(
        sectionTitle: String,
        groupingMessage: String,
        title: List<String>,
        columnSizes: List<Int>,
        countedProblems: Map<T, Int>,
        comparator: Comparator<Map.Entry<T, Int>>,
        tableRowSelector: (T) -> List<String>
    ) {
        val problemsCount = countedProblems.values.sum()

        val result = StringBuilder()
        result.appendLine(System.lineSeparator() + sectionTitle)
        result.appendLine("Analysis results: $problemsCount problems detected")

        if (problemsCount == 0) {
            cliPrinter.invoke(result.toString())
            return
        }

        val rows = countedProblems.entries.sortedWith(comparator).map { tableRowSelector.invoke(it.key) + it.value.toString() }
        val commandLineTable = CommandLineTable(
            title + "Problems count",
            rows,
            columnSizes + 0,
        )
        result.appendLine(groupingMessage)
        result.appendLine(commandLineTable.buildTable())
        cliPrinter.invoke(result.toString())
    }
}