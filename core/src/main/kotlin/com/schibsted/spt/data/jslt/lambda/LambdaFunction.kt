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
package com.schibsted.spt.data.jslt.lambda

import com.schibsted.spt.data.jslt.Parser.Companion.compileString
import com.schibsted.spt.data.jslt.impl.util.objectMapper


/**
 * A lambda function used to create the online demo playground via
 * API gateway.
 */
@Suppress("unused")
class LambdaFunction {
    /**
     * Transform the incoming JSON with JSLT and return the result.
     */
    operator fun invoke(json: String?): String {
        return try {
            // this must be:
            // {"json" : ..., "jslt" : jslt}
            val input = objectMapper.readTree(json)

            // now we can do the thing
            val source = objectMapper.readTree(input["json"].asText())
            val jslt = input["jstl"].asText()
            val template = compileString(jslt)
            val output = template.apply(source)
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output)
        } catch (e: Throwable) {
            "ERROR: $e"
        }
    }
}