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
package com.schibsted.spt.data.jslt

import com.schibsted.spt.data.jslt.impl.FunctionWrapper
import java.lang.reflect.Method

/**
 * Useful methods for working with Functions.
 */
object FunctionUtils {
    /**
     * Create a JSLT function from a static Java method. This will fail
     * if the method is overloaded.
     */
    @JvmStatic
    fun wrapStaticMethod(
        functionName: String,
        className: String,
        methodName: String
    ): Function {
        val klass = Class.forName(className)
        val methods = klass.methods
        var method: Method? = null
        for (ix in methods.indices) {
            if (methods[ix].name == methodName) {
                method =
                    if (method == null) methods[ix] else throw JsltException("More than one method named '$methodName'")
            }
        }
        if (method == null) throw JsltException("No such method: '$methodName'")
        return FunctionWrapper(functionName, method)
    }

    /**
     * Create a JSLT function from a static Java method.
     * @param paramTypes Array of types used to match overloaded methods.
     */
    @JvmStatic
    fun wrapStaticMethod(
        functionName: String,
        className: String,
        methodName: String,
        paramTypes: Array<Class<*>?>
    ): Function {
        val klass = Class.forName(className)
        val method = klass.getMethod(methodName, *paramTypes)
        return FunctionWrapper(functionName, method)
    }
}