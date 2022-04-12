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
package com.schibsted.spt.data.jslt.impl.util

import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.ResourceResolver
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class ClasspathResourceResolver @JvmOverloads constructor(private val charset: Charset = StandardCharsets.UTF_8) :
    ResourceResolver {
    override fun resolve(jslt: String): Reader {
        val inputStream = javaClass.classLoader.getResourceAsStream(jslt)
            ?: throw JsltException("Cannot load resource '$jslt': not found")
        return InputStreamReader(inputStream, charset)
    }
}