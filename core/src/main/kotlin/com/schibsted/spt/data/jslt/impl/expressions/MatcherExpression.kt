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
import com.schibsted.spt.data.jslt.impl.AbstractNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Scope

/**
 * Represents the '* - ... : .'
 */
class MatcherExpression(
    private var expr: ExpressionNode, val minuses: List<String>,
    location: Location?
) : AbstractNode(location) {
    override fun apply(scope: Scope, input: JsonNode): JsonNode {
        return expr.apply(scope, input)
    }

    override fun computeMatchContexts(parent: DotExpression) {
        // FIXME: uhhh, the rules here?
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOf(expr)
    }

    override fun dump(level: Int) {}

    override fun optimize(): ExpressionNode {
        expr = expr.optimize()
        return this
    }
}