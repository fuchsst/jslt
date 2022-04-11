package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.JsonNode

class TestNullFunction : Function {
    override val name: String
        get() = "test"
    override val minArguments: Int
        get() = 0
    override val maxArguments: Int
        get() = 0

    override fun call(input: JsonNode, params: Array<JsonNode>): JsonNode? {
        // people are not supposed to do this, but they probably will
        return null
    }
}