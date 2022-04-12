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
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.filters.JsonFilter
import com.schibsted.spt.data.jslt.impl.expressions.AbstractNode
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.expressions.LetExpression
import com.schibsted.spt.data.jslt.impl.util.convertObjectToArray
import com.schibsted.spt.data.jslt.impl.util.evalLets
import com.schibsted.spt.data.jslt.impl.util.isTrue
import com.schibsted.spt.data.jslt.impl.util.objectMapper

class ObjectComprehension(
    private var loop: ExpressionNode,
    private val lets: Array<LetExpression>,
    private var key: ExpressionNode,
    private var value: ExpressionNode,
    private var ifExpr: ExpressionNode?,
    location: Location?,
    private val filter: JsonFilter
) : AbstractNode(location) {

    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        var sequence = loop.apply(scope, input)
        if (sequence.isNull) return sequence else if (sequence.isObject) sequence =
            sequence.convertObjectToArray() else if (!sequence.isArray) throw JsltException(
            "Object comprehension can't loop over $sequence", location
        )
        val `object` = objectMapper.createObjectNode()
        for (ix in 0 until sequence.size()) {
            val context = sequence[ix]

            // must evaluate lets over again for each value because of context
            if (lets.isNotEmpty()) evalLets(scope!!, context, lets)
            if (ifExpr == null || ifExpr!!.apply(scope, context).isTrue()) {
                val valueNode = value.apply(scope, context)
                if (filter.filter(valueNode)) {
                    // if there is no value, no need to evaluate the key
                    val keyNode = key.apply(scope, context)
                    if (!keyNode.isTextual) throw JsltException(
                        "Object comprehension must have string as key, not $keyNode",
                        location
                    )
                    `object`.set<JsonNode>(keyNode.asText(), valueNode)
                }
            }
        }
        return `object`
    }

    override fun prepare(ctx: PreparationContext) {
        ctx.scope.enterScope()
        for (ix in lets.indices) lets[ix].register(ctx.scope)
        for (child in getChildren()) child.prepare(ctx)
        ctx.scope.leaveScope()
    }

    override fun getChildren(): List<ExpressionNode> {
        return lets.toList() + listOf(loop, key, value) + listOfNotNull(ifExpr)
    }

    override fun optimize(): ExpressionNode {
        for (ix in lets.indices) lets[ix].optimize()
        loop = loop.optimize()
        key = key.optimize()
        value = value.optimize()
        if (ifExpr != null) ifExpr = ifExpr!!.optimize()
        return this
    }

    override fun dump(level: Int) {}
}