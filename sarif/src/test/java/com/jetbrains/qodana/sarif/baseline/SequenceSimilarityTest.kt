package com.jetbrains.qodana.sarif.baseline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SequenceSimilarityTest {

    private fun similarity(a: String, b: String) =
        SequenceSimilarity.similarity(a.split('/'), b.split('/'))

    @Test
    fun `equal sequences score 1`() {
        assertEquals(1.0, SequenceSimilarity.similarity(listOf("a", "b", "c"), listOf("a", "b", "c")))
        assertEquals(1.0, SequenceSimilarity.similarity(emptyList(), emptyList()))
    }

    @Test
    fun `empty vs non-empty scores 0`() {
        assertEquals(0.0, SequenceSimilarity.similarity(listOf("a", "b", "c"), emptyList()))
        assertEquals(0.0, SequenceSimilarity.similarity(emptyList(), listOf("a", "b", "c")))
    }

    @Test
    fun `disjoint sequences score 0`() {
        assertEquals(0.0, similarity("a/b/c", "x/y/z"))
    }

    @Test
    fun `partial overlap`() {
        // [src, foo, Bar] vs [src, baz, Bar] -> common = {src, Bar}, similarity = 2*2/(3+3) = 0.667
        assertEquals(0.667, similarity("src/foo/Bar", "src/baz/Bar"), 0.001)
    }

    @Test
    fun `prefix match`() {
        // [a, b, c, d] vs [a, b] -> common = {a, b}, similarity = 2*2/(4+2) = 0.667
        assertEquals(0.667, similarity("a/b/c/d", "a/b"), 0.001)
    }

    @Test
    fun `lcs preserves order`() {
        // [a, b, c, d] vs [b, d] -> LCS [b, d], similarity = 2*2/(4+2) = 0.667
        assertEquals(0.667, similarity("a/b/c/d", "b/d"), 0.001)
        // [a, b, c, d] vs [d, b] -> LCS [b] or [d], length 1, similarity = 2*1/(4+2) = 0.333
        assertEquals(0.333, similarity("a/b/c/d", "d/b"), 0.001)
    }

    @Test
    fun `permuted sequences do not score 1`() {
        // [a, b, util, Foo] vs [util, a, b, Foo] -> LCS [a, b, Foo] or [util, Foo], len 3
        // similarity = 2*3/(4+4) = 0.75, not 1.0
        assertEquals(0.75, similarity("a/b/util/Foo", "util/a/b/Foo"), 0.001)
    }
}
