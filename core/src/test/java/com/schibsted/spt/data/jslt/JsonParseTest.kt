package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.ObjectMapper
import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException

/**
 * JSON parsing test cases. Verifies that Jackson and JSLT produce the
 * same JSON structure.
 */
@RunWith(Parameterized::class)
class JsonParseTest(private val json: String) {
    @Test
    fun check() {
        try {
            val expr = compileString(json)
            val actual = expr.apply(null)
            val expected = mapper.readTree(json)
            Assert.assertEquals(
                "actual class " + actual.javaClass + ", expected class " + expected.javaClass,
                expected,
                actual
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: JsltException) {
            throw RuntimeException("Parsing '$json' failed", e)
        }
    }

    companion object {
        private val mapper = ObjectMapper()
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>?> {
            val json = TestUtils.loadFile("json-parse-tests.json")
            val tests = json["tests"]
            val strings: MutableList<Array<Any>?> = ArrayList()
            for (ix in 0 until tests.size()) strings.add(arrayOf(tests[ix].asText()))
            return strings
        }
    }
}