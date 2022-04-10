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
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.NodeUtils
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.indent

/**
 * Indexing and slicing of arrays and also strings.
 */
class ArraySlicer(// can be null
    private var left: ExpressionNode?,
    private val colon: Boolean, // can be null
    private var right: ExpressionNode?,
    private var parent: ExpressionNode?,
    location: Location?
) : AbstractNode(location) {
    override fun apply(scope: Scope, input: JsonNode): JsonNode {
        val sequence = parent!!.apply(scope, input)
        if (!sequence.isArray && !sequence.isTextual) return NullNode.instance
        var size = sequence.size()
        if (sequence.isTextual) size = sequence.asText().length
        val leftix = resolveIndex(scope, left, input, size, 0)
        if (!colon) {
            return if (sequence.isArray) {
                var `val` = sequence[leftix]
                if (`val` == null) `val` = NullNode.instance
                `val`
            } else {
                val string = sequence.asText()
                if (leftix >= string.length) throw JsltException("String index out of range: $leftix", location)
                TextNode("" + string[leftix])
            }
        }
        var rightix = resolveIndex(scope, right, input, size, size)
        if (rightix > size) rightix = size
        return if (sequence.isArray) {
            val result = NodeUtils.mapper.createArrayNode()
            for (ix in leftix until rightix) result.add(sequence[ix])
            result
        } else {
            val string = sequence.asText()
            TextNode(string.substring(leftix, rightix))
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

    override fun optimize(): ExpressionNode {
        if (left != null) left = left!!.optimize()
        if (right != null) right = right!!.optimize()
        parent = parent!!.optimize()
        return this
    }

    override fun dump(level: Int) {
        if (parent != null) parent!!.dump(level)
        println(indent(level) + this)
    }

    override fun toString(): String {
        return "[$left : $right]"
    }
}