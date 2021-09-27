package com.jetbrains.qodana.sarif

import org.junit.Assert
import org.junit.Test


class TextUtilTest {
    //Issue QD-1304 & QD-1131
    @Test
    fun `have to clean 'code' tag`() {
        val input1 = "Enum entry name <code>patentLicense</code> should start with an uppercase letter"
        val input2 = "<code>if</code> statement has empty body"

        val expected1 = "Enum entry name patentLicense should start with an uppercase letter"
        val expected2 = "if statement has empty body"
        Assert.assertEquals(expected1, TextUtil.sanitizeText(input1))
        Assert.assertEquals(expected2, TextUtil.sanitizeText(input2))
    }
}