package com.jetbrains.qodana.sarif.baseline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PathSimilarityTest {

    @Test
    fun `equal paths score 1`() {
        assertEquals(1.0, PathSimilarity.similarity("a/b/c", "a/b/c"))
        assertEquals(1.0, PathSimilarity.similarity("", ""))
    }

    @Test
    fun `empty vs non-empty scores 0`() {
        assertEquals(0.0, PathSimilarity.similarity("a/b/c", ""))
        assertEquals(0.0, PathSimilarity.similarity("", "a/b/c"))
    }

    @Test
    fun `disjoint segments score 0`() {
        assertEquals(0.0, PathSimilarity.similarity("a/b/c", "x/y/z"))
    }

    @Test
    fun `partial segment overlap`() {
        // src/foo/Bar.java vs src/baz/Bar.java -> common = {src, Bar.java}, similarity = 2*2/(3+3) = 0.667
        assertEquals(0.667, PathSimilarity.similarity("src/foo/Bar.java", "src/baz/Bar.java"), 0.001)
    }

    @Test
    fun `prefix match`() {
        // a/b/c/d vs a/b -> common = {a, b}, similarity = 2*2/(4+2) = 0.667
        assertEquals(0.667, PathSimilarity.similarity("a/b/c/d", "a/b"), 0.001)
    }

    @Test
    fun `lcs preserves order`() {
        // a/b/c/d vs b/d -> LCS [b, d], similarity = 2*2/(4+2) = 0.667
        assertEquals(0.667, PathSimilarity.similarity("a/b/c/d", "b/d"), 0.001)
        // a/b/c/d vs d/b -> LCS is [b] or [d], length 1, similarity = 2*1/(4+2) = 0.333
        assertEquals(0.333, PathSimilarity.similarity("a/b/c/d", "d/b"), 0.001)
    }

    @Test
    fun `permuted paths do not score 1`() {
        // a/b/util/Foo.kt vs util/a/b/Foo.kt -> LCS [a, b, Foo.kt] or [util, Foo.kt], len 3
        // similarity = 2*3/(4+4) = 0.75, not 1.0
        assertEquals(0.75, PathSimilarity.similarity("a/b/util/Foo.kt", "util/a/b/Foo.kt"), 0.001)
    }
}
