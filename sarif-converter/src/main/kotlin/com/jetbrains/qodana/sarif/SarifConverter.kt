package com.jetbrains.qodana.sarif

import com.jetbrains.qodana.sarif.model.MetaInformation
import com.jetbrains.qodana.sarif.model.ResultAllProblems
import com.jetbrains.qodana.sarif.model.SarifReport
import java.io.File
import java.nio.file.Path

interface SarifConverter {
    fun convert(sarifFile: File, output: Path): List<File>
    fun convert(sarifReport: SarifReport, output: Path): List<File>
    fun convert(sarifFile: File): Pair<MetaInformation, ResultAllProblems>
    fun convert(sarifReport: SarifReport): Pair<MetaInformation, ResultAllProblems>
}