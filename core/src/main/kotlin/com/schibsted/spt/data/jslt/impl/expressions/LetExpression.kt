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
import com.schibsted.spt.data.jslt.impl.ScopeManager
import com.schibsted.spt.data.jslt.impl.VariableInfo
import com.schibsted.spt.data.jslt.impl.util.indent

class LetExpression(val variable: String, var declaration: ExpressionNode, location: Location?) :
    AbstractNode(location) {
    var slot: Int = ScopeManager.UNFOUND // this variable's position in the stack frame
        private set
    private var info: VariableInfo? = null

    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        return declaration.apply(scope, input)
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        declaration.computeMatchContexts(parent)
    }

    override fun dump(level: Int) {
        println(indent(level) + "let $variable =")
        declaration.dump(level + 1)
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOf(declaration)
    }

    override fun optimize(): ExpressionNode = LetExpression(
        variable = variable,
        declaration = declaration.optimize(),
        location = location
    )

    fun register(scope: ScopeManager) {
        val varInfo = scope.registerVariable(this)
        info = varInfo
        slot = varInfo.slot
    }
}


/**
 * Removes let expressions for variables that are simply assigned to
 * literals, because VariableExpression will inline those literals
 * and remove itself, so there's no need to evaluate the variable.
 */
fun Array<LetExpression>.optimize(): Array<LetExpression> {
    // return this.map { it.optimize() as LetExpression }.filterNot { it.declaration is LiteralExpression }.toTypedArray()

    var count = 0
    forEach {
        it.optimize()
        if (it.declaration !is LiteralExpression) count++
    }
    if (count == size) return this

    return filterNot { it.declaration is LiteralExpression }.toTypedArray()
}