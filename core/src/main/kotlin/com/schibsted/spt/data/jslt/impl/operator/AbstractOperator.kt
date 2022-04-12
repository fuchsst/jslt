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
package com.schibsted.spt.data.jslt.impl.operator

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.expressions.AbstractNode
import com.schibsted.spt.data.jslt.impl.expressions.DotExpression
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.expressions.LiteralExpression
import com.schibsted.spt.data.jslt.impl.util.indent

/**
 * Shared abstract superclass for comparison operators and others.
 */
abstract class AbstractOperator(
    var left: ExpressionNode, var right: ExpressionNode,
    val operator: String, location: Location?
) : AbstractNode(location) {
    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        val v1 = left.apply(scope, input)
        val v2 = right.apply(scope, input)
        return perform(v1, v2)
    }

    override fun dump(level: Int) {
        left.dump(level + 1)
        println(indent(level) + operator)
        right.dump(level + 1)
    }

    override fun optimize(): ExpressionNode {
        left = left.optimize()
        right = right.optimize()

        // if the two operands are literals we can just evaluate the
        // result right now and be done with it
        return if (left is LiteralExpression && right is LiteralExpression) LiteralExpression(
            apply(null, null),
            location
        ) else this
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        // operators are transparent to the object matcher
        left.computeMatchContexts(parent)
        right.computeMatchContexts(parent)
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOf(left, right)
    }

    abstract fun perform(v1: JsonNode, v2: JsonNode): JsonNode
    override fun toString(): String {
        val first = if (left is AbstractOperator) "($left)" else left.toString()
        val second = if (right is AbstractOperator) "($right)" else right.toString()
        return "$first $operator $second"
    }
}