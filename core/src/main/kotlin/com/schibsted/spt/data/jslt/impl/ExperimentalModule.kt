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
package com.schibsted.spt.data.jslt.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.schibsted.spt.data.jslt.Callable
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.Module
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.util.convertObjectToArray
import com.schibsted.spt.data.jslt.impl.util.objectMapper

/**
 * A module containing functions and macros that *may* be officially
 * added to JSLT in the future. For now, they're made available here
 * so that people can use them and we can build experience with these
 * implementations.
 */
class ExperimentalModule : Module {
    private val callables: MutableMap<String?, Callable?> = HashMap()
    override fun getCallable(name: String): Callable {
        return callables[name]!!
    }

    private fun register(callable: Callable) {
        callables[callable.name] = callable
    }

    class GroupBy : AbstractCallable("group-by", 3, 3), Macro {
        override fun call(
            scope: Scope, input: JsonNode,
            parameters: Array<ExpressionNode>
        ): JsonNode? {
            // this has to be a macro, because the second argument needs to be
            // evaluated in a special context

            // first find the array that we iterate over
            var array = parameters[0].apply(scope, input)
            if (array.isNull) return NullNode.instance else if (array.isObject) array =
                array.convertObjectToArray() else if (!array.isArray) throw JsltException(
                "Can't group-by on $array"
            )

            // now start grouping
            val groups: MutableMap<JsonNode?, ArrayNode?> = HashMap()
            for (ix in 0 until array.size()) {
                val groupInput = array[ix]
                val key = parameters[1].apply(scope, groupInput)
                val value = parameters[2].apply(scope, groupInput)
                var values = groups[key]
                if (values == null) {
                    values = objectMapper.createArrayNode()
                    groups[key] = values
                }
                values!!.add(value)
            }

            // grouping is done, build JSON output
            val result = objectMapper.createArrayNode()
            for (key in groups.keys) {
                val group = objectMapper.createObjectNode()
                group.set<JsonNode>("key", key)
                group.set<JsonNode>("values", groups[key])
                result.add(group)
            }
            return result
        }
    }

    companion object {
        const val URI = "http://jslt.schibsted.com/2018/experimental"
    }

    init {
        register(GroupBy())
    }
}