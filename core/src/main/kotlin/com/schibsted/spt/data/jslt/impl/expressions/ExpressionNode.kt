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
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.impl.Scope

/**
 * Internal interface for the parts of a compiled JSLT expression.
 * Different from the external interface because we want to avoid
 * having convenience methods here, and also because we may want to
 * add methods for introspection (for optimization, generating
 * byte-code, etc).
 */
interface ExpressionNode {
    fun apply(scope: Scope?, input: JsonNode?): JsonNode

    // writes debug info to stdout
    fun dump(level: Int)

    // fills in the contextQuery in ObjectExpression matchers
    fun computeMatchContexts(parent: DotExpression?)
    fun prepare(ctx: PreparationContext)

    // return self, or optimized version
    fun optimize(): ExpressionNode

    // get all direct child nodes, to reduce boilerplate in tree traversal
    fun getChildren(): List<ExpressionNode>
}