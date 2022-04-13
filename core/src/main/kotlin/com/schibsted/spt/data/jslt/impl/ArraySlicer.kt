// Copyright 2018 Schibsted Marketplaces Products & Technology As
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.schibsted.spt.data.jslt.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.expressions.AbstractNode
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.indent
import com.schibsted.spt.data.jslt.impl.util.objectMapper

/**
 * Indexing and slicing of arrays and also strings.
 */
data class ArraySlicer(// can be null
    private val left: ExpressionNode?,
    private val colon: Boolean, // can be null
    private val right: ExpressionNode?,
    private val parent: ExpressionNode?,
    override var location: Location?
) : AbstractNode(location) {

    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        val sequence = parent!!.apply(scope, input)
        if (!sequence.isArray && !sequence.isTextual) return NullNode.instance
        var size = sequence.size()
        if (sequence.isTextual) size = sequence.asText().length
        val leftIndex = resolveIndex(scope!!, left, input!!, size, 0)
        if (!colon) {
            return if (sequence.isArray) {
                var `val` = sequence[leftIndex]
                if (`val` == null) `val` = NullNode.instance
                `val`
            } else {
                val string = sequence.asText()
                if (leftIndex >= string.length) throw JsltException("String index out of range: $leftIndex", location)
                TextNode("" + string[leftIndex])
            }
        }
        var rightIndex = resolveIndex(scope, right, input, size, size)
        if (rightIndex > size) rightIndex = size
        return if (sequence.isArray) {
            val result = objectMapper.createArrayNode()
            for (ix in leftIndex until rightIndex) result.add(sequence[ix])
            result
        } else {
            val string = sequence.asText()
            TextNode(string.substring(leftIndex, rightIndex))
        }
    }

    private fun resolveIndex(
        scope: Scope, expr: ExpressionNode?,
        input: JsonNode, size: Int, ifnull: Int
    ): Int {
        if (expr == null) return ifnull
        val node = expr.apply(scope, input)
        if (!node.isNumber) throw JsltException("Can't index array/string with $node", location)
        var ix = node.intValue()
        if (ix < 0) ix += size
        return ix
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOf(parent!!) + listOfNotNull(left) + listOfNotNull(right)
    }

    override fun optimize(): ExpressionNode = copy(
        left = left?.optimize(),
        right = right?.optimize(),
        parent = parent?.optimize()
    )

    override fun dump(level: Int) {
        parent?.dump(level)
        println(indent(level) + this)
    }

    override fun toString(): String {
        return "[$left : $right]"
    }
}