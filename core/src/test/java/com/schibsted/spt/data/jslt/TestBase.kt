package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import org.junit.Assert
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.util.*

/**
 * Utilities for test cases.
 */
open class TestBase {
    fun makeVars(`var`: String?, `val`: String?): Map<String?, JsonNode?> {
        return try {
            val map: MutableMap<String?, JsonNode?> = HashMap()
            map[`var`] = mapper.readTree(`val`)
            map
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @JvmOverloads
    fun check(
        input: String?, query: String?, result: String?,
        variables: Map<String, JsonNode> = emptyMap(),
        functions: Collection<Function> = emptySet()
    ) {
        try {
            val context = mapper.readTree(input)
            val expr = compileString(query, functions)
            var actual: JsonNode? = expr.apply(variables, context)

            // reparse to handle IntNode(2) != LongNode(2)
            actual = mapper.readTree(mapper.writeValueAsString(actual))
            val expected = mapper.readTree(result)
            Assert.assertEquals(
                "actual class " + actual.javaClass + ", expected class " + expected.javaClass,
                expected,
                actual
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun load(resource: String): String {
        try {
            TestUtils::class.java.classLoader.getResourceAsStream(resource).use { stream ->
                if (stream == null) throw JsltException("Cannot load resource '$resource': not found")
                val tmp = CharArray(128)
                val reader: Reader = InputStreamReader(stream, "UTF-8")
                val buf = StringBuilder()
                while (true) {
                    val chars = reader.read(tmp, 0, tmp.size)
                    if (chars == -1) break
                    buf.append(tmp, 0, chars)
                }
                return buf.toString()
            }
        } catch (e: IOException) {
            throw JsltException("Couldn't read resource $resource", e)
        }
    }

    fun execute(input: String?, query: String?): JsonNode {
        return try {
            val context = mapper.readTree(input)
            val expr = compileString(query)
            expr.apply(context)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    // result must be contained in the error message
    fun error(query: String?, result: String?) {
        error("{}", query, result)
    }

    // result must be contained in the error message
    fun error(input: String?, query: String?, result: String?) {
        try {
            val context = mapper.readTree(input)
            val expr = compileString(query)
            expr.apply(context)
            Assert.fail("JSLT did not detect error")
        } catch (e: JsltException) {
            Assert.assertTrue(
                "incorrect error message: '" + e.message + "'",
                e.message.indexOf(result!!) != -1
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        var mapper = ObjectMapper()
    }
}