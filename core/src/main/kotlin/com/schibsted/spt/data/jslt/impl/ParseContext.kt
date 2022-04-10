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

import com.schibsted.spt.data.jslt.*
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.filters.JsonFilter
import com.schibsted.spt.data.jslt.impl.expressions.BuiltinFunctions
import com.schibsted.spt.data.jslt.impl.expressions.FunctionExpression

/**
 * Class to encapsulate context information like available functions,
 * parser/compiler settings, and so on, during parsing.
 */
class ParseContext(
    val extensions: Collection<Function>, source: String,
    resolver: ResourceResolver,
    namedModules: MutableMap<String, Module>,
    files: MutableList<JsltFile>,
    preparationContext: PreparationContext,
    objectFilter: JsonFilter
) {
    private val functions: HashMap<String, Function> = HashMap()

    /**
     * What file/resource are we parsing? Can be null, in cases where we
     * don't have this information.
     */
    val source: String

    /**
     * Imported modules listed under their prefixes. This is scoped per
     * source file, since each has a different name-module mapping.
     */
    private val modules: MutableMap<String?, Module?>

    /**
     * Tracks all loaded JSLT files. Shared between all contexts.
     */
    val files: MutableList<JsltFile>

    /**
     * Function expressions, used for delayed name-to-function resolution.
     */
    private val funcalls: MutableCollection<FunctionExpression?>
    private var parent: ParseContext? = null
    val resolver: ResourceResolver

    /**
     * Named modules listed under their identifiers.
     */
    val namedModules: MutableMap<String, Module>

    /**
     * Variable declaration and usage tracking.
     */
    val preparationContext: PreparationContext

    /**
     * Filter used to determine what object key/value pairs to keep.
     */
    val objectFilter: JsonFilter

    fun setParent(parent: ParseContext?) {
        this.parent = parent
    }

    private fun getFunction(name: String?): Function? {
        var func = functions[name]
        if (func == null) func = BuiltinFunctions.functions[name]
        return func
    }

    fun getMacro(name: String?): Macro? {
        return BuiltinFunctions.macros[name]
    }

    fun addDeclaredFunction(name: String, function: Function) {
        functions[name] = function
    }

    fun rememberFunctionCall(`fun`: FunctionExpression?) {
        funcalls.add(`fun`)
    }

    // called at the end to resolve all the functions by name
    fun resolveFunctions() {
        for (`fun` in funcalls) {
            val name = `fun`!!.functionName
            val f = getFunction(name) ?: throw JsltException(
                "No such function: '$name'",
                `fun`.getLocation()
            )
            `fun`.resolve(f)
        }
    }

    val declaredFunctions: Map<String, Function>
        get() = functions

    fun registerModule(prefix: String?, module: Module?) {
        modules[prefix] = module
    }

    fun getNamedModule(identifier: String?): Module? {
        return namedModules[identifier]
    }

    fun isAlreadyImported(module: String): Boolean {
        if (module == source) return true
        return if (parent != null) parent!!.isAlreadyImported(module) else false
    }

    fun getImportedCallable(
        prefix: String,
        name: String,
        loc: Location?
    ): Callable {
        val m = modules[prefix] ?: throw JsltException("No such module '$prefix'", loc)
        return m.getCallable(name) ?: throw JsltException("No such function '$name' in module '$prefix'", loc)
    }

    fun registerJsltFile(file: JsltFile) {
        files.add(file)
    }

    init {
        for (func in extensions) functions[func.name] = func
        this.source = source
        this.files = files
        funcalls = ArrayList()
        modules = HashMap()
        this.resolver = resolver
        this.namedModules = namedModules
        this.preparationContext = preparationContext
        this.objectFilter = objectFilter
        namedModules[ExperimentalModule.URI] = ExperimentalModule()
    }
}