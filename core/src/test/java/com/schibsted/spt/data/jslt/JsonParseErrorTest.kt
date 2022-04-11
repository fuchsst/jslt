package com.schibsted.spt.data.jslt

import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * JSON parsing test cases that are supposed to cause syntax error.
 */
@RunWith(Parameterized::class)
class JsonParseErrorTest(private val json: String) {
    @Test
    fun check() {
        try {
            compileString(json)
            Assert.fail("Successfully parsed $json")
        } catch (e: JsltException) {
            // this is what we want
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<String>> {
            val json = TestUtils.loadFile("json-parse-error-tests.json")
            val tests = json["tests"]
            val strings: MutableList<Array<String>> = ArrayList()
            for (ix in 0 until tests.size()) strings.add(arrayOf(tests[ix].asText()))
            return strings
        }
    }
}