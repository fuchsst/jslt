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
package com.schibsted.spt.data.jslt.filters

import com.fasterxml.jackson.databind.JsonNode
import com.schibsted.spt.data.jslt.Expression
import com.schibsted.spt.data.jslt.impl.util.isTrue


/**
 * Used for object filtering with JSLT expressions.
 */
class JsltJsonFilter(private val jslt: Expression) : JsonFilter {
    /**
     * Whether or not to accept this value.
     */
    override fun filter(value: JsonNode): Boolean = jslt.apply(value).isTrue()
}