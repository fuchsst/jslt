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
import com.fasterxml.jackson.databind.node.NullNode
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.util.*

class ForExpression(
    private var valueExpr: ExpressionNode,
    private val lets: Array<LetExpression>,
    private var loopExpr: ExpressionNode,
    private var ifExpr: ExpressionNode?,
    location: Location?
) : AbstractNode(location) {
    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        var array = valueExpr.apply(scope, input)
        if (array.isNull)
            return NullNode.instance
        else if (array.isObject)
            array = array.convertObjectToArray()
        else if (!array.isArray)
            throw JsltException("For loop can't iterate over $array", location)
        val result = objectMapper.createArrayNode()
        for (ix in 0 until array.size()) {
            val value = array[ix]

            // must evaluate lets over again for each value because of context
            if (lets.isNotEmpty()) evalLets(scope!!, value, lets)
            if (ifExpr == null || ifExpr!!.apply(scope, value).isTrue()) result.add(loopExpr.apply(scope, value))
        }
        return result
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        // if you do matching inside a 'for' the matching is on the
        // current object being traversed in the list. so we forget the
        // parent and start over
        loopExpr.computeMatchContexts(DotExpression(location = location))
    }

    override fun optimize(): ExpressionNode {
//        return ForExpression(
//            valueExpr = valueExpr.optimize(),
//            lets = lets.map { it.optimize() as LetExpression }.toTypedArray(),
//            loopExpr = loopExpr.optimize(),
//            ifExpr = ifExpr?.optimize(),
//            location = location
//        )
        lets.forEach { it.optimize() }
        valueExpr = valueExpr.optimize()
        loopExpr = loopExpr.optimize()
        if (ifExpr != null) ifExpr = ifExpr!!.optimize()
        return this
    }

    override fun prepare(context: PreparationContext) {
        context.scope.enterScope()
        lets.forEach { it.register(context.scope) }
        getChildren().forEach { it.prepare(context) }
        context.scope.leaveScope()
    }

    override fun getChildren(): List<ExpressionNode> {
        return lets.asList() +
                listOf(valueExpr, loopExpr) +
                listOfNotNull(ifExpr)
    }

    override fun dump(level: Int) {
        println(indent(level) + "for (")
        valueExpr.dump(level + 1)
        println(indent(level) + ")")
        loopExpr.dump(level + 1)
    }

    override fun toString(): String {
        return "[for ($valueExpr) $loopExpr" +
                (if (ifExpr != null) " if($ifExpr)" else "") +
                "]"
    }
}