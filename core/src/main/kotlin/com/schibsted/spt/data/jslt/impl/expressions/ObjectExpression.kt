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

import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.core.struct.Node
import com.schibsted.spt.data.jslt.core.struct.NullNode
import com.schibsted.spt.data.jslt.core.struct.ObjectNode
import com.schibsted.spt.data.jslt.filters.JsonFilter
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.OptimizerScope
import com.schibsted.spt.data.jslt.impl.PreparationContext
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.util.evalLets
import com.schibsted.spt.data.jslt.impl.util.indent
import com.schibsted.spt.data.jslt.impl.util.objectMapper

data class ObjectExpression(
    private val lets: Array<LetExpression>,
    private val children: Array<PairExpression>,
    private val matcher: MatcherExpression?,
    override var location: Location?,
    private val filter: JsonFilter
) : AbstractNode(location) {
    private var contextQuery: DotExpression? = null // find object to match
    private val keys: MutableSet<String?> // the static keys defined in this template

    private var containsDynamicKeys = false
    private fun checkForDuplicates() {
        val seen: MutableSet<String?> = HashSet(children.size)
        for (ix in children.indices) {
            if (seen.contains(children[ix].staticKey)) throw JsltException(
                "Invalid object declaration, duplicate key " +
                        "'" + children[ix].staticKey + "'",
                children[ix].location
            )
            seen.add(children[ix].staticKey)
        }
    }

    override fun apply(scope: Scope?, input: Node?): Node {
        evalLets(scope!!, input!!, lets)
        val `object` = objectMapper.createObjectNode()
        for (ix in children.indices) {
            val value = children[ix].apply(scope, input)
            if (filter.filter(value)) {
                val key = children[ix].applyKey(scope, input)
                if (containsDynamicKeys && `object`.has(key)) throw JsltException(
                    "Duplicate key '$key' in object",
                    children[ix].location
                )
                `object`.replace(key, value)
            }
        }
        if (matcher != null) evaluateMatcher(scope, input, `object`)
        return `object`
    }

    private fun evaluateMatcher(scope: Scope, input: Node, `object`: ObjectNode) {
        // find the object to match against
        val context = contextQuery!!.apply(scope, input)
        if (context.isNull && !context.isObject) return  // no keys to match against

        // then do the matching
        val it = context.fields()
        while (it.hasNext()) {
            val (key, value1) = it.next()
            if (keys.contains(key)) continue  // the template has defined this key, so skip
            val value = matcher!!.apply(scope, value1)
            `object`.replace(key, value)
        }
    }

    override fun computeMatchContexts(parent: DotExpression?) {
        if (matcher != null) {
            contextQuery = parent
            contextQuery!!.checkOk(location) // verify expression is legal
        }
        for (ix in lets.indices) lets[ix].computeMatchContexts(parent)
        for (ix in children.indices) children[ix].computeMatchContexts(parent)
    }

    override fun optimize(): ExpressionNode {
//        val optimizedLets = lets.map { it.optimize() as LetExpression }.toTypedArray()
//        val optimizedMatcher = matcher?.optimize() as MatcherExpression?
//        val optimizedChildren = children.map { it.optimize() as PairExpression }.toTypedArray()
//        val allLiterals = (optimizedMatcher == null && children.all { it.isLiteral }) // not static otherwise
//
//        if (!allLiterals) return copy(
//            lets = optimizedLets,
//            children = optimizedChildren,
//            matcher = optimizedMatcher,
//            location = location,
//            filter = filter
//        )
//
//        // we're a static object expression. we can just make the object and
//        // turn that into a literal, instead of creating it over and over
//        // apply parameters: literals won't use scope or input, so...
//        return LiteralExpression(apply(OptimizerScope(), NullNode.instance), location)

        for (ix in lets.indices) lets[ix].optimize()
        matcher?.optimize()
        var allLiterals = matcher == null // not static otherwise
        for (ix in children.indices) {
            children[ix] = children[ix].optimize() as PairExpression
            allLiterals = allLiterals && children[ix].isLiteral
        }
        if (!allLiterals) return this

        // we're a static object expression. we can just make the object and
        // turn that into a literal, instead of creating it over and over
        // apply parameters: literals won't use scope or input, so...
        val `object` = apply(OptimizerScope(), NullNode.instance)
        return LiteralExpression(`object`, location)
    }

    override fun prepare(context: PreparationContext) {
        context.scope.enterScope()
        lets.forEach { it.register(context.scope) }
        getChildren().forEach { it.prepare(context) }
        context.scope.leaveScope()
    }

    override fun getChildren(): List<ExpressionNode> {
        return lets.asList() + this.children.asList() + listOfNotNull(matcher)
    }

    override fun dump(level: Int) {
        println(indent(level) + '{')
        lets.forEach { it.dump(level + 1) }
        children.forEach { it.dump(level + 1) }
        println(indent(level) + '}')
    }

    init {
        keys = HashSet()
        for (ix in children.indices) {
            if (children[ix].isKeyLiteral)
                keys.add(children[ix].staticKey)
            else {
                containsDynamicKeys = true
                if (matcher != null) throw JsltException("Object matcher not allowed in objects which have dynamic keys")
            }
        }
        if (matcher != null) for (minus in matcher.minuses) keys.add(minus)
        if (!containsDynamicKeys) checkForDuplicates()
    }
}