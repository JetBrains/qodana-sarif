package com.jetbrains.qodana.sarif

import java.io.File

class Util {
    companion object {
        fun getResourceFile(filePath: String) = File(this::class.java.classLoader.getResource(filePath)!!.toURI())
    }
}