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
import com.schibsted.spt.data.jslt.impl.AbstractNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.evalLets
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.indent
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.isTrue

class IfExpression(
    private var test: ExpressionNode,
    private val thenlets: Array<LetExpression>,
    private var then: ExpressionNode,
    // can be null
    private val elselets: Array<LetExpression>?,
    // can be null
    private var orelse: ExpressionNode?,
    location: Location?
) : AbstractNode(location) {
    override fun apply(scope: Scope, input: JsonNode): JsonNode {
        if (isTrue(test.apply(scope, input))) {
            evalLets(scope, input, thenlets)
            return then.apply(scope, input)
        }

        // test was false, so return null or else
        return if (orelse != null) {
            evalLets(scope, input, elselets)
            orelse!!.apply(scope, input)
        } else NullNode.instance
    }

    override fun computeMatchContexts(parent: DotExpression) {
        for (ix in thenlets.indices) thenlets[ix].computeMatchContexts(parent)
        then.computeMatchContexts(parent)
        if (orelse != null) {
            orelse!!.computeMatchContexts(parent)
            for (ix in elselets!!.indices) elselets[ix].computeMatchContexts(parent)
        }
    }

    override fun optimize(): ExpressionNode {
        for (ix in thenlets.indices) thenlets[ix].optimize()
        if (elselets != null) {
            for (ix in elselets.indices) elselets[ix].optimize()
        }
        test = test.optimize()
        then = then.optimize()
        if (orelse != null) orelse = orelse!!.optimize()
        return this
    }

    override fun prepare(ctx: PreparationContext) {
        test.prepare(ctx)

        // then
        ctx.scope.enterScope()
        for (ix in thenlets.indices) {
            thenlets[ix].prepare(ctx)
            thenlets[ix].register(ctx.scope)
        }
        then.prepare(ctx)
        ctx.scope.leaveScope()

        // else
        if (orelse != null) {
            ctx.scope.enterScope()
            for (ix in elselets!!.indices) {
                elselets[ix].prepare(ctx)
                elselets[ix].register(ctx.scope)
            }
            orelse!!.prepare(ctx)
            ctx.scope.leaveScope()
        }
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOf(test, *thenlets, then) +
                (elselets?.toList() ?: emptyList()) +
                listOfNotNull(orelse)
    }

    override fun dump(level: Int) {
        println(indent(level) + "if (")
        test.dump(level + 1)
        println(indent(level) + ")")
        thenlets.forEach { it.dump(level + 1) }
        then.dump(level + 1)
        if (orelse != null) {
            println(indent(level) + "else")
            elselets!!.forEach { it.dump(level + 1) }
            orelse!!.dump(level + 1)
        }
    }
}