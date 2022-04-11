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
import com.schibsted.spt.data.jslt.Expression
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.expressions.DotExpression
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.expressions.LetExpression
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.evalLets

/**
 * Wrapper class that translates an external Expression to an
 * ExpressionNode.
 */
class ExpressionImpl(
    private var lets: Array<LetExpression>,
    private val functions: Map<String, Function>,
    private var actual: ExpressionNode?
) : Expression {
    private var stackFrameSize = 0
    private var fileModules: Array<JsltFile> = emptyArray()

    // contains the mapping from external parameters (variables set from
    // outside at query-time) to slots, so that we can put the
    // parameters into the scope when evaluating the query
    private var parameterSlots: Map<String, Int> = emptyMap()

    fun getFunction(name: String): Function {
        return functions[name] ?: throw JsltException("Function '$name' not found!")
    }

    fun hasBody(): Boolean {
        return actual != null
    }

    override fun apply(variables: Map<String, JsonNode>, input: JsonNode?): JsonNode {
        val scope = Scope.makeScope(variables, stackFrameSize, parameterSlots)
        return apply(scope, input)
    }

    override fun apply(input: JsonNode?): JsonNode {
        return apply(Scope.getRoot(stackFrameSize), input)
    }

    fun apply(scope: Scope, input: JsonNode?): JsonNode {
        // Jackson 2.9.2 can parse to Java null. See unit test
        // QueryTest.testNullInput. so we have to handle that
        val nullSafeInput = input ?: NullNode.instance

        // evaluate lets in global modules
        for (ix in fileModules.indices) fileModules[ix].evaluateLetsOnly(scope, nullSafeInput)

        // evaluate own lets
        evalLets(scope, nullSafeInput, lets)
        return actual!!.apply(scope, nullSafeInput)
    }

    fun dump() {
        for (ix in lets.indices) lets[ix].dump(0)
        actual!!.dump(0)
    }

    fun prepare(ctx: PreparationContext) {
        ctx.scope.enterScope()
        for (ix in lets.indices) lets[ix].register(ctx.scope)
        for (child in children) child.prepare(ctx)
        stackFrameSize = ctx.scope.stackFrameSize
        parameterSlots = ctx.scope.parameterSlots
        ctx.scope.leaveScope()
    }

    /**
     * This is used to initialize global variables when the
     * ExpressionImpl is a module. Called once during compilation.
     * The values are then remembered forever.
     */
    fun evaluateLetsOnly(scope: Scope?, input: JsonNode) {
        evalLets(scope!!, input, lets)
    }

    fun optimize() {
        lets = OptimizeUtils.optimizeLets(lets)
        functions.values
            .asSequence()
            .filterIsInstance<FunctionDeclaration>()
            .forEach { it.optimize() }
        if (actual != null) actual = actual!!.optimize()
    }

    val children: List<ExpressionNode>
        get() {
            return lets.toList() +
                    functions.values.asSequence().filterIsInstance<FunctionDeclaration>() +
                    listOfNotNull(actual)
        }

    override fun toString(): String {
        // FIXME: letexprs
        return actual.toString()
    }

    fun setGlobalModules(fileModules: List<JsltFile>) {
        this.fileModules = fileModules.toTypedArray()
    }

    init {
        // traverse tree and set up context queries
        val root = DotExpression(location = null)
        if (actual != null) actual!!.computeMatchContexts(root)
        for (ix in lets.indices) lets[ix].computeMatchContexts(root)
    }
}