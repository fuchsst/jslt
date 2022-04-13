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

import com.schibsted.spt.data.jslt.filters.DefaultJsonFilter
import com.schibsted.spt.data.jslt.filters.JsltJsonFilter
import com.schibsted.spt.data.jslt.filters.JsonFilter
import com.schibsted.spt.data.jslt.impl.JsltFile
import com.schibsted.spt.data.jslt.impl.ParseContext
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.impl.util.ClasspathResourceResolver
import com.schibsted.spt.data.jslt.parser.JsltParser
import com.schibsted.spt.data.jslt.parser.compileExpression
import java.io.*

/**
 * Parses JSLT expressions to Expression objects for evaluating them.
 * @param source A string used in error messages.
 * @param functions Extension functions
 * @param modules The keys in the map are the module "names", and importing these names will
 *                bind a prefix to the modules in this map. The names can follow any syntax.
 * @param objectFilter For all key/value pairs in objects being created, if this filter
 *                     returns false when given the value, the key/value pair is omitted.
 */
class Parser internal constructor(
    private val source: String = "<unknown>",
    private val reader: Reader, // ===== FLUENT BUILDER API
    private val functions: MutableSet<Function> = mutableSetOf(),
    private val resolver: ResourceResolver = ClasspathResourceResolver(),
    private val modules: MutableMap<String, Module> = mutableMapOf(),
    private val objectFilter: JsonFilter = DefaultJsonFilter()
) {

    /**
     * Create a new Parser with the given source name. The name is a string used in error messages.
     */
    fun withSource(thisSource: String): Parser =
        Parser(thisSource, reader, functions, resolver, modules, objectFilter)

    /**
     * Create a new Parser with the given extension functions.
     */
    fun withFunctions(theseFunctions: MutableSet<Function>): Parser =
        Parser(source, reader, theseFunctions, resolver, modules, objectFilter)

    /**
     * Create a new Parser with the given resource resolver.
     */
    fun withResourceResolver(thisResolver: ResourceResolver): Parser =
        Parser(source, reader, functions, thisResolver, modules, objectFilter)

    /**
     * Create a new Parser with the given modules registered. The keys
     * in the map are the module "names", and importing these names will
     * bind a prefix to the modules in this map. The names can follow
     * any syntax.
     */
    fun withNamedModules(thisModules: MutableMap<String, Module>): Parser {
        return Parser(
            source, reader, functions, resolver, thisModules,
            objectFilter
        )
    }

    /**
     * Create a new Parser with the given filter for object values. For
     * all key/value pairs in objects being created, if this filter
     * returns false when given the value, the key/value pair is
     * omitted.
     */
    fun withObjectFilter(filter: String?): Parser {
        val parsedFilter = compileString(filter)
        return Parser(source, reader, functions, resolver, modules, JsltJsonFilter(parsedFilter))
    }

    /**
     * Create a new Parser with the given filter for object values. For
     * all key/value pairs in objects being created, if this filter
     * returns false when given the value, the key/value pair is
     * omitted.
     */
    fun withObjectFilter(filter: JsonFilter): Parser =
        Parser(source, reader, functions, resolver, modules, filter)

    /**
     * Compile the JSLT from the defined parameters.
     */
    fun compile(): Expression {
        val context = ParseContext(
            functions,
            source,
            resolver,
            modules,
            emptyList<JsltFile>().toMutableList(),
            PreparationContext(),
            objectFilter
        )
        return context.compileExpression(JsltParser(reader))
    }

    companion object {
        /**
         * Compile the given JSLT file with the given predefined functions.
         */
        @JvmOverloads
        fun compile(jslt: File, functions: MutableSet<Function> = emptySet<Function>().toMutableSet()): Expression =
            try {
                FileReader(jslt).use { f ->
                    return Parser(source = jslt.absolutePath, functions = functions, reader = f).compile()
                }
            } catch (e: FileNotFoundException) {
                throw JsltException("Couldn't find file $jslt")
            } catch (e: IOException) {
                throw JsltException("Couldn't read file $jslt", e)
            }

        /**
         * Compile JSLT expression given as an inline string with the given
         * extension functions.
         */
        @JvmStatic
        @JvmOverloads
        fun compileString(jslt: String?, functions: MutableSet<Function> = emptySet<Function>().toMutableSet()): Expression =
            Parser(source = "<inline>", functions = functions, reader = StringReader(jslt)).compile()

        /**
         * Load and compile JSLT expression from the classpath with the
         * given extension functions.
         */
        @JvmOverloads
        fun compileResource(jslt: String, functions: MutableSet<Function> = emptySet<Function>().toMutableSet()): Expression =
            try {
                Parser::class.java.classLoader.getResourceAsStream(jslt).use { stream ->
                    if (stream == null) throw JsltException("Cannot load resource '$jslt': not found")
                    val reader: Reader = InputStreamReader(stream, "UTF-8")
                    return Parser(source = jslt, functions = functions, reader = reader).compile()
                }
            } catch (e: IOException) {
                throw JsltException("Couldn't read resource $jslt", e)
            }

        /**
         * Compile JSLT expression from the Reader.
         * @param source The source is just a name used in error messages, and has no practical effect.
         */
        @Suppress("unused")
        @JvmStatic
        fun compile(source: String, reader: Reader, functions: MutableSet<Function>): Expression =
            Parser(source = source, functions = functions, reader = reader).compile()
    }
}