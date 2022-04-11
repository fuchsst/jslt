// Copyright 2020 Schibsted Marketplaces Products & Technology As
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
package com.schibsted.spt.data.jslt.impl.operator

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.expressions.DotExpression
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode

class PipeOperator(
    left: ExpressionNode, right: ExpressionNode,
    location: Location?
) : AbstractOperator(left, right, "|", location) {
    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        return right.apply(scope, left.apply(scope, input))
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        left.computeMatchContexts(parent)
        right.computeMatchContexts(DotExpression(location =  Location(null, 0, 0)))
    }

    override fun perform(v1: JsonNode, v2: JsonNode): JsonNode {
        throw JsltException("this should NOT be reachable")
    }
}