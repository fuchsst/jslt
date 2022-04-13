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
package com.schibsted.spt.data.jslt.impl.operator.comparison

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.operator.AbstractOperator
import com.schibsted.spt.data.jslt.impl.util.toJsonNode

class EqualsComparison(
    left: ExpressionNode,
    right: ExpressionNode,
    location: Location?
) : ComparisonOperator(left, right, "==", location) {

    override fun perform(v1: JsonNode, v2: JsonNode): JsonNode = equals(v1, v2).toJsonNode()

    companion object {
        fun equals(v1: JsonNode, v2: JsonNode): Boolean {
            return if (v1.isNumber && v2.isNumber) {
                // unfortunately, comparison of numeric nodes in Jackson is
                // deliberately less helpful than what we need here. so we have
                // to develop our own support for it.
                // https://github.com/FasterXML/jackson-databind/issues/1758
                if (v1.isIntegralNumber && v2.isIntegralNumber) // if both are integers, then compare them as such
                    v1.longValue() == v2.longValue()
                else
                    v1.doubleValue() == v2.doubleValue()
            } else {
                v1 == v2
            }
        }
    }
}