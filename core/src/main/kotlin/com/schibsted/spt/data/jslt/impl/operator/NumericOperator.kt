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
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.number

abstract class NumericOperator(
    left: ExpressionNode, right: ExpressionNode, name: String,
    location: Location?
) : AbstractOperator(left, right, name, location) {

    override fun perform(v1: JsonNode, v2: JsonNode): JsonNode {
        if (v1.isNull || v2.isNull) return NullNode.instance

        val val1 = number(v1, true, location)
        val val2 = number(v2, true, location)
        return if (val1.isIntegralNumber && val2.isIntegralNumber) LongNode(
            perform(
                val1.longValue(),
                val2.longValue()
            )
        ) else DoubleNode(perform(val1.doubleValue(), val2.doubleValue()))
    }

    protected abstract fun perform(v1: Double, v2: Double): Double
    protected abstract fun perform(v1: Long, v2: Long): Long
}