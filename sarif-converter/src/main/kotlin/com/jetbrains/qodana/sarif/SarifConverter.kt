package com.jetbrains.qodana.sarif

import com.jetbrains.qodana.sarif.model.SarifReport
import java.io.File
import java.nio.file.Path

interface SarifConverter {
    fun convert(sarifFile: File, output: Path)
    fun convert(sarifReport: SarifReport, output: Path)
}