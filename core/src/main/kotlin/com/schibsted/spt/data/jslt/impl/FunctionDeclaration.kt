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
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.expressions.*
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.evalLets

class FunctionDeclaration(
    override val name: String,
    private val parameters: Array<String>,
    private val lets: Array<LetExpression>,
    private var body: ExpressionNode
) : AbstractNode(null), Function, ExpressionNode {
    private val parameterSlots: IntArray = IntArray(parameters.size)

    private var stackFrameSize = 0

    override val minArguments: Int = parameters.size
    override val maxArguments: Int=parameters.size

    // this method is here because the Function signature requires it,
    // but we can't actually use it, because a declared function needs
    // (or at least may need) access to the global scope. in order to be
    // able to treat FunctionDeclaration like other Functions we resort
    // to this solution for now.
    override fun call(input: JsonNode, arguments: Array<JsonNode>): JsonNode {
        throw JsltException("INTERNAL ERROR!")
    }

    fun call(scope: Scope, input: JsonNode, arguments: Array<JsonNode>): JsonNode {
        scope.enterFunction(stackFrameSize)

        // bind the arguments into the function scope
        for (ix in arguments.indices) scope.setValue(parameterSlots[ix], arguments[ix])

        // then bind the lets
        evalLets(scope, input, lets)

        // evaluate body
        val value = body.apply(scope, input)
        scope.leaveFunction()
        return value
    }

    override fun optimize(): ExpressionNode {
        for (ix in lets.indices) lets[ix].optimize()
        body = body.optimize()
        return this
    }

    // the ExpressionNode API requires this method, but it doesn't
    // actually make any sense for a Function
    override fun apply(scope: Scope?, context: JsonNode?): JsonNode {
        throw JsltException("INTERNAL ERROR")
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        // not allowed to use object matcher inside declared functions
        val fail = FailDotExpression(null, "function declaration")
        for (ix in lets.indices) lets[ix].computeMatchContexts(fail)
        body.computeMatchContexts(fail)
    }

    override fun prepare(ctx: PreparationContext) {
        ctx.scope.enterFunction()
        for (ix in parameters.indices) parameterSlots[ix] = ctx.scope.registerParameter(parameters[ix], location)
        for (ix in lets.indices) {
            lets[ix].register(ctx.scope)
            lets[ix].prepare(ctx)
        }
        body.prepare(ctx)
        stackFrameSize = ctx.scope.stackFrameSize
        ctx.scope.leaveFunction()
    }

}