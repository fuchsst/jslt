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
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.util.evalLets
import com.schibsted.spt.data.jslt.impl.util.indent
import com.schibsted.spt.data.jslt.impl.util.isTrue

class IfExpression(
    private var test: ExpressionNode,
    private val thenLets: Array<LetExpression>,
    private var then: ExpressionNode,
    // can be null
    private val elseLets: Array<LetExpression>?,
    // can be null
    private var orElse: ExpressionNode?,
    location: Location?
) : AbstractNode(location) {

    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        if (test.apply(scope, input).isTrue()) {
            evalLets(scope!!, input!!, thenLets)
            return then.apply(scope, input)
        }

        // test was false, so return null or else
        return if (orElse != null) {
            evalLets(scope!!, input!!, elseLets)
            orElse!!.apply(scope, input)
        } else NullNode.instance
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        for (ix in thenLets.indices) thenLets[ix].computeMatchContexts(parent)
        then.computeMatchContexts(parent)
        if (orElse != null) {
            orElse!!.computeMatchContexts(parent)
            for (ix in elseLets!!.indices) elseLets[ix].computeMatchContexts(parent)
        }
    }

    override fun optimize(): ExpressionNode {
//        copy(
//            test = test.optimize(),
//            thenLets = thenLets.map { it.optimize() as LetExpression }.toTypedArray(),
//            then = then.optimize(),
//            elseLets = elseLets?.map { it.optimize() as LetExpression }?.toTypedArray(),
//            orElse = orElse?.optimize()
//        )

        thenLets.forEach { it.optimize() }
        elseLets?.forEach { it.optimize()            }
        test = test.optimize()
        then = then.optimize()
        if (orElse != null) orElse = orElse!!.optimize()
        return this
    }

    override fun prepare(context: PreparationContext) {
        test.prepare(context)

        // then
        context.scope.enterScope()
        for (ix in thenLets.indices) {
            thenLets[ix].prepare(context)
            thenLets[ix].register(context.scope)
        }
        then.prepare(context)
        context.scope.leaveScope()

        // else
        if (orElse != null) {
            context.scope.enterScope()
            for (ix in elseLets!!.indices) {
                elseLets[ix].prepare(context)
                elseLets[ix].register(context.scope)
            }
            orElse!!.prepare(context)
            context.scope.leaveScope()
        }
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOf(test, *thenLets, then) +
                (elseLets?.toList() ?: emptyList()) +
                listOfNotNull(orElse)
    }

    override fun dump(level: Int) {
        println(indent(level) + "if (")
        test.dump(level + 1)
        println(indent(level) + ")")
        thenLets.forEach { it.dump(level + 1) }
        then.dump(level + 1)
        if (orElse != null) {
            println(indent(level) + "else")
            elseLets!!.forEach { it.dump(level + 1) }
            orElse!!.dump(level + 1)
        }
    }
}