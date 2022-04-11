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
package com.schibsted.spt.data.jslt.impl.operator

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.number

abstract class ComparisonOperator(
    left: ExpressionNode, right: ExpressionNode,
    operator: String, location: Location?
) : AbstractOperator(left, right, operator, location) {
    abstract override fun perform(v1: JsonNode, v2: JsonNode): JsonNode

    companion object {
        fun compare(v1: JsonNode, v2: JsonNode, location: Location?): Double {
            if (v1.isNumber && v2.isNumber) {
                val n1 = number(v1, location).doubleValue()
                val n2 = number(v2, location).doubleValue()
                return n1 - n2
            } else if (v1.isTextual && v2.isTextual) {
                val s1 = v1.asText()
                val s2 = v2.asText()
                return s1.compareTo(s2).toDouble()
            } else if (v1.isNull || v2.isNull) {
                // null is equal to itself, and considered the smallest of all
                return if (v1.isNull && v2.isNull)
                    0.0
                else if (v1.isNull)
                    (-1).toDouble()
                else
                    1.0
            }
            throw JsltException("Can't compare $v1 and $v2", location)
        }
    }
}