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

import com.schibsted.spt.data.jslt.Callable
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.core.struct.ArrayNode
import com.schibsted.spt.data.jslt.core.struct.Node
import com.schibsted.spt.data.jslt.core.struct.NullNode
import com.schibsted.spt.data.jslt.impl.*
import com.schibsted.spt.data.jslt.impl.util.asString

class FunctionExpression(
    val functionName: String,
    arguments: Array<ExpressionNode>,
    location: Location?,
) : AbstractInvocationExpression(arguments, location) {
    private var function: Function? = null // null before resolution
    private var declared: FunctionDeclaration? = null // non-null if a declared function


    override fun resolve(callable: Callable) {
        super.resolve(callable)
        function = callable as Function
        if (callable is FunctionDeclaration) declared = callable
    }

    override fun apply(scope: Scope?, input: Node?): Node {
        val params = arguments.map { it.apply(scope, input) }.toTypedArray()

        return if (declared != null) declared!!.call(scope!!, input!!, params) else {
            // if the user-implemented function returns Java null, silently
            // turn it into a JSON null. (the alternative is to throw an
            // exception.)
            function!!.call(input!!, params) ?: NullNode.instance
        }
    }

    override fun optimize(): ExpressionNode {
        val optimizeArrayContainsMin = 10
        arguments = arguments.map { it.optimize() }.toTypedArray()

        // if the second argument to contains() is an array with a large
        // number of elements, don't do a linear search. instead, use an
        // optimized version of the function that uses a HashSet
        if (function === BuiltinFunctions.functions["contains"] && arguments.size == 2 &&
            arguments[1] is LiteralExpression
        ) {
            val v = arguments[1].apply(null, null)
            if (v is ArrayNode && v.values.size > optimizeArrayContainsMin) {
                // we use resolve to make sure all references are updated
                resolve(OptimizedStaticContainsFunction(v))
            }
        }

        // ensure compile-time evaluation of static regular expressions
        if (function is RegexpFunction) {
            val index = (function as RegexpFunction).regexpArgumentNumber()
            if (arguments[index] is LiteralExpression) {
                val r = arguments[index].apply(null, null).asString(JsltException("Regexp cannot be null"))

                // will fill in cache, and throw correct exception
                BuiltinFunctions.getRegexp(r)
            }
        }
        return this
    }

    override fun toString(): String {
        return super.toString()
    }

}