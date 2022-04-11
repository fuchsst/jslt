package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.ObjectMapper
import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Checks that JSLT queries produce certain runtime errors.
 */
@RunWith(Parameterized::class)
class QueryErrorTest(private val input: String, private val query: String, private val error: String) : TestBase() {
    @Test
    fun check() {
        try {
            val context = mapper.readTree(input)
            val expr = compileString(query)
            expr.apply(context)
            Assert.fail("JSLT did not detect error in $query")
        } catch (e: JsltException) {
            Assert.assertTrue(
                "incorrect error message: '" + e.message + "', " +
                        "correct: '" + error + "'",
                e.message.indexOf(error) != -1
            )
        } catch (e: Exception) {
            throw RuntimeException("Failure on query $query: $e", e)
        }
    }

    companion object {
        private val mapper = ObjectMapper()

        @JvmStatic
        @Parameterized.Parameters(name = "query: {1}")
        fun data(): Collection<Array<Any>?> {
            val strings: MutableList<Array<Any>?> = ArrayList()
            strings.addAll(loadTests("query-error-tests.json"))
            strings.addAll(loadTests("function-error-tests.json"))
            strings.addAll(loadTests("function-declaration-tests.yaml"))
            return strings
        }

        private fun loadTests(resource: String): Collection<Array<Any>?> {
            val json = TestUtils.loadFile(resource)
            val tests = json["tests"]
            val strings: MutableList<Array<Any>?> = ArrayList()
            for (ix in 0 until tests.size()) {
                val test = tests[ix]
                if (!test.has("error")) // not an error test, so skip it
                // this works because we load the same file in QueryTest
                    continue
                strings.add(
                    arrayOf(
                        test["input"].asText(),
                        test["query"].asText(),
                        test["error"].asText()
                    )
                )
            }
            return strings
        }
    }
}