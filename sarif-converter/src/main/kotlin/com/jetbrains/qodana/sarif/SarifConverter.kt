package com.jetbrains.qodana.sarif

import com.jetbrains.qodana.sarif.model.MetaInformation
import com.jetbrains.qodana.sarif.model.ResultAllProblems
import java.io.File

interface SarifConverter {
    fun convert(sarifFile: File): Pair<MetaInformation, ResultAllProblems>
}