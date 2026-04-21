package com.jetbrains.qodana.sarif.baseline

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LcsSimilarityTest {

    @Test
    fun lcsLength() {
        assertEquals(0, LcsSimilarity.lcsLength("", ""))
        assertEquals(0, LcsSimilarity.lcsLength("abc", ""))
        assertEquals(0, LcsSimilarity.lcsLength("", "abc"))
        assertEquals(0, LcsSimilarity.lcsLength("abc", "xyz"))
        assertEquals(3, LcsSimilarity.lcsLength("abc", "abc"))
        assertEquals(3, LcsSimilarity.lcsLength("abcde", "ace"))
        assertEquals(4, LcsSimilarity.lcsLength("ABCBDAB", "BDCAB"))
    }

    @Test
    fun similarity() {
        assertEquals(1.0, LcsSimilarity.similarity("", ""))
        assertEquals(0.0, LcsSimilarity.similarity("abc", ""))
        assertEquals(0.0, LcsSimilarity.similarity("", "abc"))
        assertEquals(0.0, LcsSimilarity.similarity("abc", "xyz"))
        assertEquals(1.0, LcsSimilarity.similarity("abc", "abc"))
        assertEquals(0.75, LcsSimilarity.similarity("abcde", "ace"), 0.001)
    }

    @Test
    fun isSubsequence() {
        assertTrue(LcsSimilarity.isSubsequence("", "anything"))
        assertTrue(LcsSimilarity.isSubsequence("", ""))
        assertTrue(LcsSimilarity.isSubsequence("ace", "abcde"))
        assertTrue(LcsSimilarity.isSubsequence("abc", "abc"))
        assertFalse(LcsSimilarity.isSubsequence("aec", "abcde"))
        assertFalse(LcsSimilarity.isSubsequence("abcdef", "abc"))
    }
}
