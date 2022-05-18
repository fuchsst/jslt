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

import com.fasterxml.jackson.databind.node.*
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.core.struct.*
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.asNullableString
import com.schibsted.spt.data.jslt.impl.util.number
import com.schibsted.spt.data.jslt.impl.util.objectMapper

class PlusOperator(
    left: ExpressionNode, right: ExpressionNode,
    location: Location?
) : NumericOperator(left, right, "+", location) {

    override fun perform(v1: Node, v2: Node): Node {
       return try {
           v1 + v2
       } catch (e:Exception) {
           throw JsltException("Can't compare $v1 and $v2", location)
       }
    }

    override fun perform(v1: Double, v2: Double): Double = v1 + v2

    override fun perform(v1: Long, v2: Long): Long = v1 + v2

}


operator fun Node.plus(other:Node): Node {
    return when {
        this.isTextual || other.isTextual -> {
            // if one operand is string: do string concatenation
            TextNode((this.asNullableString() ?: "null") + (other.asNullableString() ?: "null"))
        }
        this.isArray && other.isArray // if both are arrays: array concatenation
        -> objectMapper.createArrayNode()
            .addAll(this as ArrayNode)
            .addAll(other as ArrayNode)
        this.isObject && other.isObject // if both are objects: object union
        -> objectMapper.createObjectNode()
            .setAll<ObjectNode>(other as ObjectNode)
            .setAll(this as ObjectNode)
        (this.isObject || this.isArray) && other.isNull -> this
        this.isNull && (other.isObject || other.isArray) -> other
        else  // do numeric operation
        -> {
            if (this.isNull || other.isNull) return NullNode.instance

            val val1 = number(this, true, null)
            val val2 = number(other, true, null)
            return if (val1.isIntegralNumber && val2.isIntegralNumber)
                LongNode(this.longValue() + other.longValue())
            else
                DoubleNode(this.doubleValue() + val2.doubleValue())
        }
    }
}