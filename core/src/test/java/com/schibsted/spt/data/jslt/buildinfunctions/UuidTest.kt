package com.schibsted.spt.data.jslt.buildinfunctions

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import com.schibsted.spt.data.jslt.TestBase
import org.junit.Assert
import org.junit.Test

class UuidTest : TestBase() {
    @Test
    @Throws(JsonProcessingException::class)
    fun testUuidWithoutParameterMatchesRegex() {
        val given = compileString("uuid()")
        val actual = mapper.writeValueAsString(given.apply(null))
        val uuidRegex = "^\"[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\"$".toRegex()
        Assert.assertTrue(actual.matches(uuidRegex))
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun testUuidWithoutParameterGeneratesRandomValues() {
        val given = compileString("{ \"uuid1\" : uuid(), \"uuid2\" : uuid() }")
        val result = given.apply(null)
        val actual1 = mapper.writeValueAsString(result.findValue("uuid1"))
        val actual2 = mapper.writeValueAsString(result.findValue("uuid2"))
        Assert.assertNotEquals(actual1, actual2)
    }

    @Test(expected = JsltException::class)
    fun testUuidWithOneParameterRaisesJsltException() {
        val given = compileString("uuid(123)")
        given.apply(null)
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}