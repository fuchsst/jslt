package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.*
import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import com.schibsted.spt.data.jslt.filters.TrueJsonFilter
import com.schibsted.spt.data.jslt.impl.ModuleImpl
import com.schibsted.spt.data.jslt.impl.util.ClasspathResourceResolver
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.IOException
import java.io.StringReader
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Tests that cannot be expressed in JSON.
 */
class StaticTests : TestBase() {
    @Test
    fun testExceptionWithNoLocation() {
        try {
            val expr = compileString("contains(2, 2)")
            val actual = expr.apply(null)
        } catch (e: JsltException) {
            Assert.assertTrue(e.getSource() == null)
            Assert.assertEquals(-1, e.getLine().toLong())
            Assert.assertEquals(-1, e.getColumn().toLong())
        }
    }

    @Test
    fun testObjectKeyOrder() {
        val expr = compileString("{\"a\":1, \"b\":2}")
        val actual = expr.apply(null)
        val it = actual.fieldNames()
        Assert.assertEquals("a", it.next())
        Assert.assertEquals("b", it.next())
    }

    @Test
    fun testRandomFunction() {
        try {
            val context = mapper.readTree("{}")
            val expr = compileString("random()")
            for (ix in 0..9) {
                val actual = expr.apply(context)
                Assert.assertTrue(actual.isNumber)
                val value = actual.doubleValue()
                Assert.assertTrue(value > 0.0 && value < 1.0)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun testJavaExtensionFunction() {
        check("{}", "test()", "42", emptyMap(), setOf(TestFunction()))
    }

    @Test
    fun testJavaExtensionFunctionNull() {
        check("{}", "test()", "null", emptyMap(), setOf(TestNullFunction()))
    }

    @Test
    fun testJavaExtensionFunctionNullInExpression() {
        check("{}", "test() or 42", "true", emptyMap(), setOf(TestNullFunction()))
    }

    @Test
    fun testJavaExtensionFunctionNullInExpression2() {
        check("{}", "lowercase(test())", "null", emptyMap(), setOf(TestNullFunction()))
    }

    @Test
    fun testNowFunction() {
        val now1 = execute("{}", "now()")
        val now2 = System.currentTimeMillis().toDouble()
        val delta: Long = 1000 // milliseconds of wriggle-room
        Assert.assertTrue(now1!!.isDouble)
        Assert.assertTrue(
            "now1 ($now1) << now2 ($now2)",
            now1.asDouble() * 1000 < now2 + delta
        )
        Assert.assertTrue(
            "now1 ($now1) >> now2 ($now2)",
            now1.asDouble() * 1000 > now2 - delta
        )
    }

    @Test
    fun testIsDecimalFunction() {
        // check that is-decimal still works even if input
        // is a FloatNode and not a DoubleNode
        val expr = compileString("is-decimal(.)")
        val context: JsonNode = FloatNode(1.0f)
        val actual = expr.apply(context)
        Assert.assertTrue(actual.isBoolean)
        Assert.assertTrue(actual.booleanValue())
    }

    @Test
    fun testIsIntegerFunction() {
        // check that is-integer still works if input
        // is a BigIntegerNode not just IntNode
        val expr = compileString("is-integer(.)")
        val context: JsonNode = BigIntegerNode(BigInteger.ONE)
        val actual = expr.apply(context)
        Assert.assertTrue(actual.isBoolean)
        Assert.assertTrue(actual.booleanValue())
    }

    @Test
    @Ignore // this takes a while to run, so we don't usually do it
    fun testRegexpCache() {
        // generate lots and lots of regular expressions, and see if we
        // manage to blow up the cache
        val expr = compileString("capture(\"foo\", .)")
        for (ix in 0..9999999) {
            val r = generateRegexp()
            val regexp: JsonNode = TextNode(r)
            expr.apply(regexp)
        }
    }

    private fun generateRegexp(): String {
        if (Math.random() < 0.3) {
            // generate compound expression
            val parts = (Math.random() * 5).toInt()
            val buf = StringBuilder()
            buf.append("(")
            for (ix in 0 until parts) {
                buf.append(generateRegexp())
                if (ix + 1 < parts) buf.append("|")
            }
            buf.append(")")
            return buf.toString()
        } else {
            // generate simple expression
            val kind = (Math.random() * 4).toInt()
            when (kind) {
                0 -> return "[A-Za-z0-9]+"
                1 -> return makeRandomString(10)
                2 -> return "\\d+"
                3 -> return "20\\d\\d-[01]\\d-[0123]\\d"
            }
        }
        return "foo"
    }

    private fun makeRandomString(length: Int): String {
        return (0 until length).map { 'a' + (Math.random()*26).toInt().toChar().code }.joinToString(separator = "")
    }

    @Test
    fun testNamedModule() {
        val functions: MutableMap<String, Function> = HashMap()
        functions["test"] = TestFunction()
        val module = ModuleImpl(functions)
        val modules: MutableMap<String, Module> = HashMap()
        modules["the test module"] = module
        val jslt = StringReader(
            "import \"the test module\" as t t:test()"
        )
        val expr = Parser(jslt)
            .withNamedModules(modules)
            .compile()
        val result = expr.apply(null)
        Assert.assertEquals(IntNode(42), result)
    }

    @Test
    @Throws(IOException::class)
    fun testJsltObjectFilter() {
        // filter to accept everything that isn't null
        val filter = " . != null "
        val jslt = StringReader(
            "{ \"foo\" : null, \"bar\" : \"\" }"
        )
        val expr = Parser(jslt)
            .withObjectFilter(filter)
            .compile()
        val desired = mapper.readTree(
            "{ \"bar\" : \"\" }"
        )
        val result = expr.apply(null)
        Assert.assertEquals(desired, result)
    }

    @Test
    @Throws(IOException::class)
    fun testJsltObjectFilter2() {
        // filter to accept everything that isn't the empty string
        val filter = " . != \"\" "
        val jslt = StringReader(
            "{ \"foo\" : null, \"bar\" : \"\" }"
        )
        val expr = Parser(jslt)
            .withObjectFilter(filter)
            .compile()
        val desired = mapper.readTree(
            "{ \"foo\" : null }"
        )
        val result = expr.apply(null)
        Assert.assertEquals(desired, result)
    }

    @Test
    @Throws(IOException::class)
    fun testJsltObjectFilter3() {
        // filter to accept everything that isn't the empty string
        val filter = " . != \"\" "
        val jslt = StringReader(
            "{for (.) .key : .value }"
        )
        val expr = Parser(jslt)
            .withObjectFilter(filter)
            .compile()
        val input = mapper.readTree(
            "{ \"foo\" : null, \"bar\" : \"\" }"
        )
        val desired = mapper.readTree(
            "{ \"foo\" : null }"
        )
        val result = expr.apply(input)
        Assert.assertEquals(desired, result)
    }

    @Test
    @Throws(IOException::class)
    fun testTrueObjectFilter() {
        val jslt = StringReader(
            "{for (.) .key : .value }"
        )
        val expr = Parser(jslt)
            .withObjectFilter(TrueJsonFilter())
            .compile()
        val input = mapper.readTree(
            "{ \"foo\" : null, \"bar\" : \"\" }"
        )
        val desired = mapper.readTree(
            "{ \"foo\" : null, \"bar\" : \"\" }"
        )
        val result = expr.apply(input)
        Assert.assertEquals(desired, result)
    }

    @Test
    fun testTrailingCommasInObject() {
        val expr = compileString("{\"a\":1, \"b\":2,}")
        val actual = expr.apply(null)
        val it = actual.fieldNames()
        Assert.assertEquals("a", it.next())
        Assert.assertEquals("b", it.next())
    }

    @Test
    fun testTrailingCommasInArray() {
        val expr = compileString("[1,2,]")
        val actual = expr.apply(null) as ArrayNode
        Assert.assertEquals(2, actual.size().toLong())
        Assert.assertEquals(1, actual[0].asInt().toLong())
        Assert.assertEquals(2, actual[1].asInt().toLong())
    }

    @Test
    fun testClasspathResolverCharEncoding() {
        val r = ClasspathResourceResolver(StandardCharsets.ISO_8859_1)
        val expr = Parser(r.resolve("character-encoding-master.jslt"))
            .withResourceResolver(r)
            .compile()
        val result = expr.apply(NullNode.instance)
        Assert.assertEquals("Hei på deg", result.asText())
    }

    @Test
    @Throws(IOException::class)
    fun testPipeOperatorAndObjectMatcher() {
        val expr = compileString("{\"bar\": \"baz\",\"foo\":{ \"a\": \"b\" } | {\"type\" : \"Anonymized-View\",* : .}}")
        val desired = mapper.readTree(
            "{\"bar\":\"baz\",\"foo\":{\"type\":\"Anonymized-View\",\"a\":\"b\"}}"
        )
        val result = expr.apply(null)
        Assert.assertEquals(desired, result)
    }

    @Test
    @Throws(IOException::class)
    fun testTestFunctionCompileFail() {
        // we want to verify that this function fails at compile-time
        // not at runtime
        try {
            compileString("test(., \"\\\\\")")
            Assert.fail("Accepted static, invalid regular expression")
        } catch (e: JsltException) {
            Assert.assertTrue(e.message.indexOf("regular expression") != -1)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testCaptureFunctionCompileFail() {
        // we want to verify that this function fails at compile-time
        // not at runtime
        try {
            compileString("capture(., \"\\\\\")")
            Assert.fail("Accepted static, invalid regular expression")
        } catch (e: JsltException) {
            Assert.assertTrue(e.message.indexOf("regular expression") != -1)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSplitFunctionCompileFail() {
        // we want to verify that this function fails at compile-time
        // not at runtime
        try {
            compileString("split(., \"\\\\\")")
            Assert.fail("Accepted static, invalid regular expression")
        } catch (e: JsltException) {
            Assert.assertTrue(e.message.indexOf("regular expression") != -1)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testReplaceFunctionCompileFail() {
        // we want to verify that this function fails at compile-time
        // not at runtime
        try {
            compileString("replace(., \"\\\\\", \"something\")")
            Assert.fail("Accepted static, invalid regular expression")
        } catch (e: JsltException) {
            Assert.assertTrue(e.message.indexOf("regular expression") != -1)
        }
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}