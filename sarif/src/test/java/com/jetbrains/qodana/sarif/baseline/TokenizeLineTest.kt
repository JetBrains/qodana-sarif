package com.jetbrains.qodana.sarif.baseline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for [tokenizeLine]: whitespace separates and is dropped, every delimiter char becomes its own token,
 * and maximal runs of the remaining characters form word tokens.
 */
class TokenizeLineTest {

    @Test
    fun `blank input yields no tokens`() {
        assertEquals(emptyList<String>(), tokenizeLine(""))
        assertEquals(emptyList<String>(), tokenizeLine("   \t "))
    }

    @Test
    fun `whitespace separates words and is dropped`() {
        assertEquals(listOf("foo", "bar", "baz"), tokenizeLine("foo   bar\tbaz"))
    }

    @Test
    fun `each delimiter becomes its own token`() {
        assertEquals(listOf("foo", "(", "bar", ",", "baz", ")"), tokenizeLine("foo(bar, baz)"))
    }

    @Test
    fun `adjacent delimiters each stand alone`() {
        assertEquals(listOf(")", "]", "}"), tokenizeLine(")]}"))
    }

    @Test
    fun `code punctuation splits into tokens`() {
        assertEquals(listOf("a", "=", "b;"), tokenizeLine("a = b;")) // ';' is not a delimiter, stays in the word
        assertEquals(listOf("map", "[", "key", "]", ".", "value"), tokenizeLine("map[key].value"))
        assertEquals(listOf("foo", "(", "x", ")", "{"), tokenizeLine("foo(x) {"))
    }

    @Test
    fun `quotes and slashes are not delimiters and stay within a word`() {
        assertEquals(listOf("say", "\"hi\""), tokenizeLine("say \"hi\""))
        assertEquals(listOf("a/b\\c"), tokenizeLine("a/b\\c"))
    }

    @Test
    fun `characters outside the delimiter set stay within a word`() {
        // '-', '_', '@' and digits are not delimiters, so they remain part of the surrounding word.
        assertEquals(listOf("foo_bar-baz@1"), tokenizeLine("foo_bar-baz@1"))
    }

    @Test
    fun `words split on camelCase boundaries`() {
        assertEquals(listOf("test", "Word"), tokenizeLine("testWord"))
        assertEquals(listOf("get", "HTTP", "Response"), tokenizeLine("getHTTPResponse"))
        assertEquals(listOf("HTTP", "Server"), tokenizeLine("HTTPServer"))
        assertEquals(listOf("test2", "Word"), tokenizeLine("test2Word"))
        assertEquals(listOf("alllower"), tokenizeLine("alllower"))
    }

    @Test
    fun `camelCase split composes with delimiter split`() {
        assertEquals(listOf("get", "Foo", "(", "bar", "Baz", ")"), tokenizeLine("getFoo(barBaz)"))
    }
}
