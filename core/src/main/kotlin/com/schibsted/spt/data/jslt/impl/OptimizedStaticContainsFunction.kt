// Copyright 2019 Schibsted Marketplaces Products & Technology As
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
package com.schibsted.spt.data.jslt.impl

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.impl.AbstractFunction
import com.fasterxml.jackson.databind.node.BooleanNode
import java.util.HashSet

/**
 * An optimized version of contains(a, b) which is used when b is an
 * array literal with a large number of values, so that a linear
 * search becomes a performance drag.
 */
class OptimizedStaticContainsFunction(array: JsonNode) : AbstractFunction("optimized-static-contains", 2, 2) {
    private val values: Set<JsonNode?> = array.elements().asSequence().toSet()

    override fun call(input: JsonNode, arguments: Array<JsonNode>): JsonNode {
        return if (values.contains(arguments[0])) BooleanNode.TRUE else BooleanNode.FALSE
    }
}