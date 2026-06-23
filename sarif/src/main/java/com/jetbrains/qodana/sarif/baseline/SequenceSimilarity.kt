package com.jetbrains.qodana.sarif.baseline

/** Order-preserving token-sequence similarity: `2·LCS / (|a| + |b|)`, in `[0, 1]`. */
internal object SequenceSimilarity {

    fun similarity(a: List<String>, b: List<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return 2.0 * lcsLength(a, b) / (a.size + b.size)
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
