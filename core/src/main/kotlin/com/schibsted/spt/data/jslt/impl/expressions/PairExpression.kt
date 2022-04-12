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
package com.schibsted.spt.data.jslt.impl.expressions

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.util.indent

/**
 * Represents a ("key" : expr) pair inside a JSON object.
 */
class PairExpression(private var key: ExpressionNode, private var value: ExpressionNode, location: Location?) :
    AbstractNode(location) {

    fun applyKey(scope: Scope?, input: JsonNode?): String {
        val v = key.apply(scope, input)
        if (!v.isTextual) {
            throw JsltException("Object key must be string", location)
        }
        return v.asText()
    }

    val staticKey: String
        get() {
            if (!isKeyLiteral) throw JsltException("INTERNAL ERROR: Attempted to get non-static key")
            return key.apply(null, null).asText()
        }

    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        return value.apply(scope, input)
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        // a pair that has a dynamic key cannot use matching in the value
        val expr: DotExpression = if (isKeyLiteral)
            DotExpression(staticKey, parent, location)
        else
            FailDotExpression(location, "dynamic object")
        value.computeMatchContexts(expr)
    }

    val isLiteral: Boolean
        get() = value is LiteralExpression && key is LiteralExpression
    val isKeyLiteral: Boolean
        get() = key is LiteralExpression

    override fun optimize(): ExpressionNode {
        key = key.optimize()
        value = value.optimize()
        return this
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOf(key, value)
    }

    override fun dump(level: Int) {
        println(indent(level) + '"' + key + '"' + " :")
        value.dump(level + 1)
    }
}