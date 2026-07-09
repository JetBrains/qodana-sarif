package com.jetbrains.qodana.sarif.baseline

/** Precomputed ASCII lookup array for O(1) delimiter checks */
private val DELIMITER_FLAGS = BooleanArray(128).apply {
    ".(),=[]{}".forEach { this[it.code] = true }
}

private fun isDelimiter(ch: Char): Boolean = ch.code < 128 && DELIMITER_FLAGS[ch.code]

/** Splits a line into tokens in a single pass over the characters, emitting substrings straight from [line] */
internal fun tokenizeLine(line: String): List<String> {
    val tokens = ArrayList<String>()
    var wordStart = -1

    fun flushWord(end: Int) {
        if (wordStart != -1) {
            tokens.add(line.substring(wordStart, end))
            wordStart = -1
        }
    }

    for (i in line.indices) {
        val ch = line[i]
        when {
            ch.isWhitespace() -> flushWord(i)
            isDelimiter(ch) -> { flushWord(i); tokens.add(ch.toString()) }
            wordStart == -1 -> wordStart = i // start of a word
            isCamelBoundary(line, i) -> { tokens.add(line.substring(wordStart, i)); wordStart = i }
        }
    }
    flushWord(line.length)

    return tokens
}

/** camelCase split points:
 * lowercase/digit -> uppercase (`testWord`),
 * and an acronym's last uppercase before a lowercase (`HTTPServer`)
 * */
private fun isCamelBoundary(line: String, i: Int): Boolean {
    val prev = line[i - 1]
    val curr = line[i]
    return (prev.isLowerCase() || prev.isDigit()) && curr.isUpperCase() ||
            (prev.isUpperCase() && curr.isUpperCase() && i + 1 < line.length && line[i + 1].isLowerCase())
}
