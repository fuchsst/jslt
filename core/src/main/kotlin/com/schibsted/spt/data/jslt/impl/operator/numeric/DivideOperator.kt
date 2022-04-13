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
package com.schibsted.spt.data.jslt.impl.operator.numeric

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.number

class DivideOperator(
    left: ExpressionNode, right: ExpressionNode,
    location: Location?
) : NumericOperator(left, right, "/", location) {
    override fun perform(v1: JsonNode, v2: JsonNode): JsonNode {
        if (v1.isNull || v2.isNull) return NullNode.instance

        // we only support the numeric operation and nothing else
        val dividend = number(v1, true, location)
        val divisor = number(v2, true, location)
        return if (dividend.isIntegralNumber && divisor.isIntegralNumber) {
            val l1 = dividend.longValue()
            val l2 = divisor.longValue()
            if (l1 % l2 == 0L) LongNode(l1 / l2) else DoubleNode(l1.toDouble() / l2.toDouble())
        } else DoubleNode(perform(dividend.doubleValue(), divisor.doubleValue()))
    }

    override fun perform(v1: Double, v2: Double): Double = v1 / v2

    // can't use this, because the integers are not closed under division
    override fun perform(v1: Long, v2: Long): Long = v1 / v2 // uhh ... ?
}