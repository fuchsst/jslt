package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode

class TestFunction : Function {
    override val name: String
        get() = "test"
    override val minArguments: Int
        get() = 0
    override val maxArguments: Int
        get() = 0

    override fun call(input: JsonNode, arguments: Array<JsonNode>): JsonNode? {
        return IntNode(42)
    }
}