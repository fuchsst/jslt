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

import com.schibsted.spt.data.jslt.impl.util.ClasspathResourceResolver
import com.schibsted.spt.data.jslt.filters.DefaultJsonFilter
import com.schibsted.spt.data.jslt.filters.JsltJsonFilter
import com.schibsted.spt.data.jslt.impl.ParseContext
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.parser.JsltParser
import kotlin.jvm.JvmOverloads
import com.schibsted.spt.data.jslt.filters.JsonFilter
import com.schibsted.spt.data.jslt.impl.JsltFile
import com.schibsted.spt.data.jslt.parser.ParserImpl
import java.io.*

/**
 * Parses JSLT expressions to Expression objects for evaluating them.
 */
class Parser private constructor(
    private val source: String, private val reader: Reader, // ===== FLUENT BUILDER API
    private val functions: Collection<Function>,
    private val resolver: ResourceResolver,
    private val modules: MutableMap<String, Module>,
    private val objectFilter: JsonFilter
) {
    /**
     * Create a Parser reading JSLT source from the given Reader. Uses a
     * [ClasspathResourceResolver] for import statements.
     */
    constructor(reader: Reader) : this(
        "<unknown>", reader, emptySet<Function>().toMutableSet(),
        ClasspathResourceResolver(), emptyMap<String, Module>().toMutableMap(),
        DefaultJsonFilter()
    )

    /**
     * Create a new Parser with the given source name. The name is a string
     * used in error messages.
     */
    fun withSource(thisSource: String): Parser {
        return Parser(
            thisSource, reader, functions, resolver, modules,
            objectFilter
        )
    }

    /**
     * Create a new Parser with the given extension functions.
     */
    fun withFunctions(theseFunctions: Collection<Function>): Parser {
        return Parser(
            source, reader, theseFunctions, resolver, modules,
            objectFilter
        )
    }

    /**
     * Create a new Parser with the given resource resolver.
     */
    fun withResourceResolver(thisResolver: ResourceResolver): Parser {
        return Parser(
            source, reader, functions, thisResolver, modules,
            objectFilter
        )
    }

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
        return Parser(
            source, reader, functions, resolver, modules,
            JsltJsonFilter(parsedFilter)
        )
    }

    /**
     * Create a new Parser with the given filter for object values. For
     * all key/value pairs in objects being created, if this filter
     * returns false when given the value, the key/value pair is
     * omitted.
     */
    fun withObjectFilter(filter: JsonFilter): Parser {
        return Parser(
            source, reader, functions, resolver, modules,
            filter
        )
    }

    /**
     * Compile the JSLT from the defined parameters.
     */
    fun compile(): Expression {
        val ctx = ParseContext(
            functions, source, resolver, modules,
            emptyList<JsltFile>().toMutableList(),
            PreparationContext(),
            objectFilter
        )
        return ParserImpl.compileExpression(ctx, JsltParser(reader))
    }

    companion object {
        /**
         * Compile the given JSLT file with the given predefined functions.
         */
        @JvmOverloads
        fun compile(jslt: File, functions: Collection<Function> =emptySet()): Expression {
            try {
                FileReader(jslt).use { f ->
                    return Parser(f)
                        .withSource(jslt.absolutePath)
                        .withFunctions(functions)
                        .compile()
                }
            } catch (e: FileNotFoundException) {
                throw JsltException("Couldn't find file $jslt")
            } catch (e: IOException) {
                throw JsltException("Couldn't read file $jslt", e)
            }
        }
        /**
         * Compile JSLT expression given as an inline string with the given
         * extension functions.
         */
        @JvmStatic
        @JvmOverloads
        fun compileString(
            jslt: String?,
            functions: Collection<Function> = emptySet()
        ): Expression {
            return Parser(StringReader(jslt))
                .withSource("<inline>")
                .withFunctions(functions)
                .compile()
        }
        /**
         * Load and compile JSLT expression from the classpath with the
         * given extension functions.
         */
        @JvmOverloads
        fun compileResource(
            jslt: String,
            functions: Collection<Function> = emptySet()
        ): Expression {
            try {
                Parser::class.java.classLoader.getResourceAsStream(jslt).use { stream ->
                    if (stream == null) throw JsltException("Cannot load resource '$jslt': not found")
                    val reader: Reader = InputStreamReader(stream, "UTF-8")
                    return Parser(reader)
                        .withSource(jslt)
                        .withFunctions(functions)
                        .compile()
                }
            } catch (e: IOException) {
                throw JsltException("Couldn't read resource $jslt", e)
            }
        }

        /**
         * Compile JSLT expression from the Reader. The source is just a
         * name used in error messages, and has no practical effect.
         */
        @JvmStatic
        fun compile(
            source: String,
            reader: Reader,
            functions: Collection<Function>
        ): Expression {
            return Parser(reader)
                .withSource(source)
                .withFunctions(functions)
                .compile()
        }
    }
}