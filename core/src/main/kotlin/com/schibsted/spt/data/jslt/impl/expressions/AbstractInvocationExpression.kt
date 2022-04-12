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
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.util.indent

/**
 * Common superclass for function and macro expressions, to avoid
 * having to repeat so much code.
 */
abstract class AbstractInvocationExpression(
    @JvmField protected var arguments: Array<ExpressionNode>,
    location: Location?
) : AbstractNode(location) {
    private var callable: Callable? = null // null until resolve is called


    // invoked when we know which callable it's going to be
    open fun resolve(callable: Callable) {
        this.callable = callable
        if (arguments.size < callable.minArguments || arguments.size > callable.maxArguments) {
            val kind = if (this is FunctionExpression) "Function" else "Macro"
            throw JsltException(
                "$kind '${callable.name}' " +
                        "needs ${callable.minArguments}-${callable.maxArguments} arguments, " +
                        "got ${arguments.size}", location
            )
        }
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        arguments.forEach { arg ->
            arg.computeMatchContexts(parent)
        }
    }

    override fun optimize(): ExpressionNode {
        arguments.indices.forEach { ix ->
            arguments[ix] = arguments[ix].optimize()
        }
        return this
    }

    override fun dump(level: Int) {
        println(indent(level) + callable!!.name + "(")
        arguments.forEach { it.dump(level + 1) }
        println(indent(level) + ')')
    }

    override fun getChildren(): List<ExpressionNode> {
        return arguments.toList()
    }

    override fun toString(): String {
        return StringBuilder().apply {
            append(callable!!.name)
            append('(')
            arguments.forEachIndexed { ix, node ->
                if (ix > 0) append(", ")
                append(node.toString())
            }
            append(')')
        }.toString()
    }
}