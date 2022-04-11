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

import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.impl.util.NodeUtils.indent

abstract class AbstractNode(var location: Location?) : ExpressionNode {
    override fun dump(level: Int) {
        println(indent(level) + this)
    }

    override fun computeMatchContexts(parent: DotExpression?) {}

    override fun prepare(ctx: PreparationContext) {
        getChildren().forEach { it.prepare(ctx) }
    }

    override fun optimize(): ExpressionNode = this

    override fun getChildren(): List<ExpressionNode> = emptyList()

}