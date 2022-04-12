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
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.util.indent
import com.schibsted.spt.data.jslt.impl.util.objectMapper

class ArrayExpression(private val children: Array<ExpressionNode>, location: Location?) : AbstractNode(location) {
    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        return objectMapper.createArrayNode().addAll(
            children.map { it.apply(scope, input) }
        )
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        val fail = FailDotExpression(location, "array")
        for (ix in children.indices) children[ix].computeMatchContexts(fail)
    }

    override fun getChildren(): List<ExpressionNode> {
        return children.toList()
    }

    override fun optimize(): ExpressionNode {
        var allLiterals = true
        for (ix in children.indices) {
            children[ix] = children[ix].optimize()
            allLiterals = allLiterals && children[ix] is LiteralExpression
        }
        if (!allLiterals) return this

        // we're a static array expression. we can just make the array and
        // turn that into a literal, instead of creating it over and over
        val array = apply(null, null) // literals won't use scope or input
        return LiteralExpression(array, location)
    }

    override fun dump(level: Int) {
        println(indent(level) + '[')
        for (ix in children.indices) children[ix].dump(level + 1)
        println(indent(level) + ']')
    }
}