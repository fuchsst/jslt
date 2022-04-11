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
import com.schibsted.spt.data.jslt.impl.*
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.indent

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

    override fun optimize(): ExpressionNode {
        declaration = declaration.optimize()
        return this
    }

    fun register(scope: ScopeManager) {
        val varInfo = scope.registerVariable(this)
        info = varInfo
        slot = varInfo.slot
    }

}