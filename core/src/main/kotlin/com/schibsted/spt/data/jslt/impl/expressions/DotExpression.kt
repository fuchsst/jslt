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

import com.schibsted.spt.data.jslt.core.struct.Node
import com.schibsted.spt.data.jslt.core.struct.NullNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.util.indent

open class DotExpression(
    private var key: String? = null,
    private var parent: ExpressionNode? = null,
    override var location: Location?
) : AbstractNode(location) {

    override fun apply(scope: Scope?, input: Node?): Node {
        // if there is no key we just return the input
        if (key == null) return input!!

        // if we have a parent, get the input from the parent (preceding expr)
        val lookupNode = if (parent != null) parent!!.apply(scope, input) else input!!

        // okay, do the keying
        return lookupNode[key] ?: NullNode.instance
    }

    override fun getChildren(): List<ExpressionNode> {
        return listOfNotNull(parent)
    }

    override fun dump(level: Int) {
        println(indent(level) + this)
    }

    override fun toString(): String {
        val me = "." + if (key == null) "" else key
        return if (parent != null) "" + parent + me else me
    }

    // verify that we've build a correct DotExpression for our object
    // matcher (only used for that)
    open fun checkOk(matcher: Location?) {
        // this object is OK, but might be a FailDotExpression higher up,
        // so check for that
        if (parent != null) (parent as DotExpression).checkOk(matcher)
    }

    override fun optimize(): ExpressionNode = DotExpression(
        key = key,
        parent = parent?.optimize(),
        location=location
    )
}