package com.jetbrains.qodana.sarif.model

import org.jetbrains.teamcity.qodana.json.Severity
import org.jetbrains.teamcity.qodana.json.version3.Problem
import org.jetbrains.teamcity.qodana.json.version3.SimpleProblem

data class ResultAllProblems(val version: String = "3", val listProblem: List<Problem>) {
    companion object {
        internal fun emptySimpleProblem() = SimpleProblem("", "", "", null, Severity.TYPO, "", "", mutableListOf(), null, 0)
    }
}
