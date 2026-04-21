package com.jetbrains.qodana.sarif.baseline

internal object LcsSimilarity {

    fun lcsLength(a: String, b: String): Int {
        if (a.isEmpty() || b.isEmpty()) return 0
        val (short, long) = if (a.length <= b.length) a to b else b to a
        val dp = IntArray(short.length + 1)
        for (i in long.indices) {
            var prevDiag = 0
            for (j in short.indices) {
                val temp = dp[j + 1]
                dp[j + 1] = if (long[i] == short[j]) prevDiag + 1
                else maxOf(dp[j], dp[j + 1])
                prevDiag = temp
            }
        }
        return dp[short.length]
    }

    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return 2.0 * lcsLength(a, b) / (a.length + b.length)
    }

    fun isSubsequence(part: String, whole: String): Boolean {
        if (part.isEmpty()) return true
        var pi = 0
        for (ch in whole) {
            if (ch == part[pi]) {
                pi++
                if (pi == part.length) return true
            }
        }
        return false
    }
}
