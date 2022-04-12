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

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Macro
import com.schibsted.spt.data.jslt.impl.Scope

// not sure we actually need this ExpressionNode class. maybe macros
// should be expressions in their own right? it does mean we get to
// do the parameter count checking just once, though. we also need to
// see whether macros are going to be an external or internal feature.
class MacroExpression(
    macro: Macro, arguments: Array<ExpressionNode>,
    location: Location?
) : AbstractInvocationExpression(arguments, location) {
    private val macro: Macro

    override fun apply(scope: Scope?, input: JsonNode?): JsonNode {
        return macro.call(scope!!, input!!, arguments)!!
    }

    init {
        resolve(macro)
        this.macro = macro
    }
}