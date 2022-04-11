package com.schibsted.spt.data.jslt

import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import org.junit.Assert
import org.junit.Test

/**
 * Verifying that toString works as it should.
 */
class ToStringTest : TestBase() {
    // ----- DOT EXPRESSIONS
    @Test
    fun testDot() {
        verify(".", ".")
    }

    @Test
    fun testDotKey() {
        verify(".key", ".key")
    }

    @Test
    fun testDotKeyDotKey() {
        verify(".key.foo", ".key.foo")
    }

    // ----- LITERALS
    @Test
    fun testNumber() {
        verify("22", "22")
    }

    // ----- FUNCTIONS
    @Test
    fun testFunction0() {
        verify("now()", "now()")
    }

    @Test
    fun testFunction() {
        verify("is-number(22)", "is-number(22)")
    }

    @Test
    fun testFunctionIsInteger() {
        verify("is-integer(22)", "is-integer(22)")
    }

    @Test
    fun testFunctionIsDecimal() {
        verify("is-decimal(22.0)", "is-decimal(22.0)")
    }

    @Test
    fun testFunctionAny() {
        verify("all([true, false])", "all([true,false])")
    }

    @Test
    fun testFunctionAll() {
        verify("any([true, false])", "any([true,false])")
    }

    @Test
    fun testFunction2() {
        verify("number(\"22\", null)", "number(\"22\", null)")
    }

    // ----- MACROS
    @Test
    fun testMacro2() {
        verify("fallback(\"22\", null)", "fallback(\"22\", null)")
    }

    // ----- OPERATORS
    @Test
    fun testTwoPlusTwo() {
        verify("2+\$v", "2 + \$v")
    }

    // ----- FOR
    @Test
    fun testFor() {
        verify("[for (.foo) .bar]", "[for (.foo) .bar]")
    }

    // ----- VARIABLE
    @Test
    fun testVariable() {
        verify("\$foo", "\$foo")
    }

    // ----- UTILITIES
    private fun verify(input: String, output: String) {
        val expr = compileString(input)
        val actual = expr.toString()
        Assert.assertEquals(output, actual)
    }
}