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
import com.schibsted.spt.data.jslt.impl.*
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.indent

class VariableExpression(val variable: String, location: Location?) : AbstractNode(location) {
    private var slot: Int = ScopeManager.UNFOUND
    private var info: VariableInfo? = null

    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        return scope?.getValue(slot) ?: throw JsltException("No such variable '$variable'", location)
    }

    override fun dump(level: Int) {
        println(indent(level) + this)
    }

    override fun prepare(ctx: PreparationContext) {
        val varInfo = ctx.scope.resolveVariable(this)
        slot = varInfo.slot
        varInfo.incrementUsageCount()
        info = varInfo
    }

    override fun optimize(): ExpressionNode {
        // if the variable is assigned to a literal then there's no point
        // in actually having a variable. we can just insert the literal
        // in the expression tree and be done with it.
        val declaration = info!!.declaration
        // will be null if the variable is a parameter
        return if (declaration != null && declaration is LiteralExpression) declaration else this
    }

    override fun toString(): String {
        return "$$variable"
    }

}