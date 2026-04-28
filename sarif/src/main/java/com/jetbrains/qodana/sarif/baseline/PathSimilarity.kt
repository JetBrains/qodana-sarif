package com.jetbrains.qodana.sarif.baseline

internal object PathSimilarity {

    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val sa = a.split('/')
        val sb = b.split('/')
        return 2.0 * lcsLength(sa, sb) / (sa.size + sb.size)
    }

    private fun lcsLength(a: List<String>, b: List<String>): Int {
        val (short, long) = if (a.size <= b.size) a to b else b to a
        val dp = IntArray(short.size + 1)
        for (item in long) {
            var prevDiag = 0
            for (j in short.indices) {
                val temp = dp[j + 1]
                dp[j + 1] = if (item == short[j]) prevDiag + 1 else maxOf(dp[j], dp[j + 1])
                prevDiag = temp
            }
        }
        return dp[short.size]
    }
}
