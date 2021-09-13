package com.jetbrains.qodana.sarif

import java.io.File
import java.nio.file.Path

class Util {
    companion object {
        fun getResourceFile(filePath: String) = File(this::class.java.classLoader.getResource(filePath)!!.toURI())
        fun readFileAsText(filePath: String) = this::class.java.getResource(filePath)!!.readText()
        fun Path.readText() = this.toUri().toURL().readText()
    }
}