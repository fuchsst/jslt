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

import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode

/**
 * Class encapsulating what we know about a specific variable. Keeps
 * track of the stack frame slot, but mostly used for optimizations.
 */
abstract class VariableInfo(val location: Location?) {
    var slot = 0
    var usageCount = 0 // how many references to this variable?
        private set
    abstract val name: String

    fun incrementUsageCount() {
        usageCount++
    }

    open val isLet: Boolean
        get() = false

    /**
     * The expression that computes this variable's value. null for
     * parameters, because in that case we don't know the expression.
     */
    open val declaration: ExpressionNode?
        get() = null
}