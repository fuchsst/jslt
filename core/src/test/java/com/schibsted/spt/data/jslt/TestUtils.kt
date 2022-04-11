package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader

object TestUtils {
    private val jsonMapper = ObjectMapper()
    private val yamlMapper = ObjectMapper(
        YAMLFactory()
    )

    fun loadFile(resource: String): JsonNode {
        try {
            TestUtils::class.java.classLoader.getResourceAsStream(resource).use { stream ->
                if (stream == null) throw JsltException("Cannot load resource '$resource': not found")
                val reader: Reader = InputStreamReader(stream, "UTF-8")
                return if (resource.endsWith(".json")) jsonMapper.readTree(reader) else if (resource.endsWith(".yaml")) yamlMapper.readTree(
                    reader
                ) else throw JsltException(
                    "Unknown format: $resource"
                )
            }
        } catch (e: IOException) {
            throw JsltException("Couldn't read resource $resource", e)
        }
    }
}