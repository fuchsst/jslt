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
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.asNullableString
import com.schibsted.spt.data.jslt.impl.util.objectMapper

class PlusOperator(
    left: ExpressionNode, right: ExpressionNode,
    location: Location?
) : NumericOperator(left, right, "+", location) {

    override fun perform(v1: JsonNode, v2: JsonNode): JsonNode {
        return when {
            v1.isTextual || v2.isTextual -> {
                // if one operand is string: do string concatenation
                TextNode((v1.asNullableString() ?: "null") + (v2.asNullableString() ?: "null"))
            }
            v1.isArray && v2.isArray // if both are arrays: array concatenation
            -> concatenateArrays(v1, v2)
            v1.isObject && v2.isObject // if both are objects: object union
            -> unionObjects(v1, v2)
            (v1.isObject || v1.isArray) && v2.isNull -> v1
            v1.isNull && (v2.isObject || v2.isArray) -> v2
            else  // do numeric operation
            -> super.perform(v1, v2)
        }
    }

    override fun perform(v1: Double, v2: Double): Double = v1 + v2

    override fun perform(v1: Long, v2: Long): Long = v1 + v2

    private fun concatenateArrays(v1: JsonNode, v2: JsonNode): ArrayNode =
        objectMapper.createArrayNode()
            .addAll(v1 as ArrayNode)
            .addAll(v2 as ArrayNode)

    private fun unionObjects(v1: JsonNode, v2: JsonNode): ObjectNode =
        objectMapper.createObjectNode()
        .setAll<ObjectNode>(v2 as ObjectNode)
        .setAll(v1 as ObjectNode)
}