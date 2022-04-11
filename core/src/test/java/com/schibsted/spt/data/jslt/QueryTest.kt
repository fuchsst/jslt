package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test cases verifying queries against an input.
 */
@RunWith(Parameterized::class)
class QueryTest(
    private val input: String, private val query: String, private val output: String,
    private val variables: Map<String, JsonNode>
) : TestBase() {
    @Test
    fun check() {
        try {
            val context = mapper.readTree(input)
            val expr = compileString(query)
            var actual: JsonNode? = expr.apply(variables, context)

            // reparse to handle IntNode(2) != LongNode(2)
            actual = mapper.readTree(mapper.writeValueAsString(actual))
            val expected = mapper.readTree(output)
            Assert.assertEquals(
                "" + expected + " != " + actual + " in query " + query + ", input: " + input + ", actual class " + actual.javaClass + ", expected class " + expected.javaClass,
                expected,
                actual
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
            strings.addAll(loadTests("query-tests.json"))
            strings.addAll(loadTests("query-tests.yaml"))
            strings.addAll(loadTests("function-tests.json"))
            strings.addAll(loadTests("experimental-tests.json"))
            strings.addAll(loadTests("function-declaration-tests.yaml"))
            return strings
        }

        private fun loadTests(resource: String): Collection<Array<Any>?> {
            val json = TestUtils.loadFile(resource)
            val tests = json["tests"]
            val strings: MutableList<Array<Any>?> = ArrayList()
            for (ix in 0 until tests.size()) {
                val test = tests[ix]
                if (!test.has("output")) // not a query test, so skip it
                // this works because we load the same file in QueryErrorTest
                    continue
                strings.add(
                    arrayOf(
                        test["input"].asText(),
                        test["query"].asText(),
                        test["output"].asText(),
                        toMap(test["variables"])
                    )
                )
            }
            return strings
        }

        private fun toMap(json: JsonNode?): Map<String?, JsonNode?> {
            val variables: MutableMap<String?, JsonNode?> = HashMap()
            if (json != null) {
                val it = json.fieldNames()
                while (it.hasNext()) {
                    val field = it.next()
                    variables[field] = json[field]
                }
            }
            return variables
        }
    }
}