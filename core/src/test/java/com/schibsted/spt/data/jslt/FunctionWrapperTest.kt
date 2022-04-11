package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.FunctionUtils.wrapStaticMethod
import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import org.junit.Assert
import org.junit.Test

/**
 * Test cases for the function wrapper implementations.
 */
class FunctionWrapperTest : TestBase() {
    @Test
    @Throws(Exception::class)
    fun testWrapStaticMethod() {
        val functions: Collection<Function> = setOf(
            wrapStaticMethod(
                "url-decode",
                "java.net.URLDecoder", "decode", arrayOf(String::class.java, String::class.java)
            )
        )
        check(
            "{}", "url-decode(\"foo\", \"utf-8\")", "\"foo\"",
            emptyMap(),
            functions
        )
    }

    @Test
    @Throws(Exception::class)
    fun testWrapStaticMethodNotFound() {
        try {
            wrapStaticMethod(
                "url-decode",
                "java.net.URLDecoder", "decooode"
            )
            Assert.fail("accepted non-existent method")
        } catch (e: JsltException) {
            // this is what we expected
        }
    }

    @Test
    @Throws(Exception::class)
    fun testWrapStaticMethodLong() {
        val functions: Collection<Function> = setOf(
            wrapStaticMethod(
                "time-millis",
                "java.lang.System", "currentTimeMillis"
            )
        )
        val query = "time-millis()"
        val before = System.currentTimeMillis()
        val context: JsonNode = mapper.readTree("{}")
        val expr = compileString(query, functions)
        val actual = expr.apply(context)
        val value = actual.asLong()
        val after = System.currentTimeMillis()
        Assert.assertTrue(before <= value)
        Assert.assertTrue(value <= after)
    }

    @Test
    @Throws(Exception::class)
    fun testWrapStaticMethodNumeric() {
        val functions: Collection<Function> = setOf(
            wrapStaticMethod(
                "pow",
                "java.lang.Math", "pow"
            )
        )
        val query = "pow(2, 10)"
        val context: JsonNode = mapper.readTree("{}")
        val expr = compileString(query, functions)
        val actual = expr.apply(context)
        Assert.assertTrue(actual.asInt() == 1024)
    }
}