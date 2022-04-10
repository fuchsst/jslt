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
package com.schibsted.spt.data.jslt.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import com.schibsted.spt.data.jslt.Expression
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.*
import com.schibsted.spt.data.jslt.impl.expressions.*
import com.schibsted.spt.data.jslt.impl.operator.*
import java.io.IOException

object ParserImpl {
    fun compileExpression(ctx: ParseContext, parser: JsltParser): Expression {
        return try {
            parser.Start()
            //((SimpleNode) parser.jjtree.rootNode()).dump("");
            val expr = compile(ctx, parser.jjtree.rootNode() as SimpleNode)
            expr.setGlobalModules(ctx.files)
            expr
        } catch (e: ParseException) {
            throw JsltException(
                "Parse error: " + e.message,
                makeLocation(ctx, e.currentToken)
            )
        } catch (e: TokenMgrError) {
            throw JsltException("Parse error: " + e.message)
        }
    }

    private fun compileImport(
        functions: Collection<Function>,
        parent: ParseContext,
        jslt: String
    ): ExpressionImpl {
        try {
            parent.resolver.resolve(jslt).use { reader ->
                val ctx = ParseContext(
                    functions,
                    jslt,
                    parent.resolver,
                    parent.namedModules,
                    parent.files,
                    parent.preparationContext,
                    parent.objectFilter
                )
                ctx.setParent(parent)
                return compileModule(ctx, JsltParser(reader))
            }
        } catch (e: IOException) {
            throw JsltException("Couldn't read resource $jslt", e)
        }
    }

    private fun compileModule(ctx: ParseContext, parser: JsltParser): ExpressionImpl {
        return try {
            parser.Module()
            compile(ctx, parser.jjtree.rootNode() as SimpleNode)
        } catch (e: ParseException) {
            throw JsltException(
                "Parse error: " + e.message,
                makeLocation(ctx, e.currentToken)
            )
        } catch (e: TokenMgrError) {
            throw JsltException("Parse error: " + e.message)
        }
    }

    private fun compile(ctx: ParseContext, root: SimpleNode): ExpressionImpl {
        processImports(ctx, root) // registered with context
        val lets = buildLets(ctx, root)
        collectFunctions(ctx, root) // registered with context
        val expr = getLastChild(root)
        var top: ExpressionNode? = null
        if (expr!!.id == JsltParserTreeConstants.JJTEXPR) top = node2expr(ctx, expr)
        ctx.resolveFunctions()
        val impl = ExpressionImpl(lets, ctx.declaredFunctions, top)
        impl.prepare(ctx.preparationContext)
        impl.optimize()
        return impl
    }

    private fun node2expr(ctx: ParseContext, node: SimpleNode?): ExpressionNode {
        if (node!!.id != JsltParserTreeConstants.JJTEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
        var root = node2orexpr(ctx, getChild(node, 0))
        var ix = 0
        while (node.jjtGetNumChildren() > ix * 2 + 1) {
            val child1 = getChild(node, 2 + ix * 2)
            val next = node2orexpr(ctx, child1)

            // get the operator
            val loc = makeLocation(ctx, node)
            val child2 = getChild(node, 1 + ix * 2)
            val comp = child2.jjtGetFirstToken()
            root = if (comp.kind == JsltParserConstants.PIPE) PipeOperator(
                root,
                next,
                loc
            ) else throw JsltException("INTERNAL ERROR: What kind of operator is this?")
            ix += 1
        }
        return root
    }

    private fun node2orexpr(ctx: ParseContext, node: SimpleNode): ExpressionNode {
        if (node.id != JsltParserTreeConstants.JJTOREXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
        val first = node2andexpr(ctx, getChild(node, 0))
        if (node.jjtGetNumChildren() == 1) // it's just the base
            return first
        val second = node2orexpr(ctx, getChild(node, 1))
        return OrOperator(
            first,
            second,
            makeLocation(ctx, node)
        )
    }

    private fun node2andexpr(ctx: ParseContext, node: SimpleNode): ExpressionNode {
        if (node.id != JsltParserTreeConstants.JJTANDEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
        val first = node2compexpr(ctx, getChild(node, 0))
        if (node.jjtGetNumChildren() == 1) // it's just the base
            return first
        val second = node2andexpr(ctx, getChild(node, 1))
        return AndOperator(
            first,
            second,
            makeLocation(ctx, node)
        )
    }

    private fun node2compexpr(ctx: ParseContext, node: SimpleNode): ExpressionNode {
        if (node.id != JsltParserTreeConstants.JJTCOMPARATIVEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
        val first = node2addexpr(ctx, getChild(node, 0))
        if (node.jjtGetNumChildren() == 1) // it's just the base
            return first
        val second = node2addexpr(ctx, getChild(node, 2))

        // get the comparator
        val loc = makeLocation(ctx, node)
        val comp = getChild(node, 1).jjtGetFirstToken()
        return when (comp.kind) {
            JsltParserConstants.EQUALS -> EqualsComparison(
                first,
                second,
                loc
            )
            JsltParserConstants.UNEQUALS -> UnequalsComparison(
                first,
                second,
                loc
            )
            JsltParserConstants.BIGOREQ -> BiggerOrEqualComparison(
                first,
                second,
                loc
            )
            JsltParserConstants.BIGGER -> BiggerComparison(
                first,
                second,
                loc
            )
            JsltParserConstants.SMALLER -> SmallerComparison(
                first,
                second,
                loc
            )
            JsltParserConstants.SMALLOREQ -> SmallerOrEqualsComparison(
                first,
                second,
                loc
            )
            else -> throw JsltException("INTERNAL ERROR: What kind of comparison is this? $node")
        }
    }

    private fun node2addexpr(ctx: ParseContext, node: SimpleNode): ExpressionNode {
        if (node.id != JsltParserTreeConstants.JJTADDITIVEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
        var root = node2mulexpr(ctx, getChild(node, 0))
        var ix = 0
        while (node.jjtGetNumChildren() > ix * 2 + 1) {
            val next = node2mulexpr(ctx, getChild(node, 2 + ix * 2))

            // get the operator
            val loc = makeLocation(ctx, node)
            val comp = getChild(node, 1 + ix * 2).jjtGetFirstToken()
            root = when (comp.kind) {
                JsltParserConstants.PLUS -> PlusOperator(
                    root,
                    next,
                    loc
                )
                JsltParserConstants.MINUS -> MinusOperator(
                    root,
                    next,
                    loc
                )
                else -> throw JsltException("INTERNAL ERROR: What kind of operator is this?")
            }
            ix += 1
        }
        return root
    }

    private fun node2mulexpr(ctx: ParseContext, node: SimpleNode): ExpressionNode {
        if (node.id != JsltParserTreeConstants.JJTMULTIPLICATIVEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
        var root = node2baseExpr(ctx, getChild(node, 0))
        var ix = 0
        while (node.jjtGetNumChildren() > ix * 2 + 1) {
            val child1 = getChild(node, 2 + ix * 2)
            val next = node2baseExpr(ctx, child1)

            // get the operator
            val loc = makeLocation(ctx, node)
            val child2 = getChild(node, 1 + ix * 2)
            val comp = child2.jjtGetFirstToken()
            root = when (comp.kind) {
                JsltParserConstants.STAR -> MultiplyOperator(
                    root,
                    next,
                    loc
                )
                JsltParserConstants.SLASH -> DivideOperator(
                    root,
                    next,
                    loc
                )
                else -> throw JsltException("INTERNAL ERROR: What kind of operator is this?")
            }
            ix += 1
        }
        return root
    }

    private fun node2baseExpr(ctx: ParseContext, node: SimpleNode): ExpressionNode {
        var localNode = node
        if (localNode.id != JsltParserTreeConstants.JJTBASEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $localNode")
        val loc = makeLocation(ctx, localNode)
        var token = localNode.jjtGetFirstToken()
        if (token.kind == JsltParserConstants.LBRACKET || token.kind == JsltParserConstants.LCURLY || token.kind == JsltParserConstants.IF) // it's not a token but a production, so we ditch the Expr node
        // and go down to the level below, which holds the actual info
            localNode = localNode.jjtGetChild(0) as SimpleNode
        token = localNode.jjtGetFirstToken()
        val kind = token.kind
        return if (kind == JsltParserConstants.NULL) LiteralExpression(
            NullNode.instance,
            loc
        ) else if (kind == JsltParserConstants.INTEGER) {
            val numberObj: JsonNode
            val number = token.image.toLong()
            numberObj = if (number > Int.MAX_VALUE || number < Int.MIN_VALUE) LongNode(number) else IntNode(
                number.toInt()
            )
            LiteralExpression(numberObj, loc)
        } else if (kind == JsltParserConstants.DECIMAL) {
            val number = DoubleNode(token.image.toDouble())
            LiteralExpression(number, loc)
        } else if (kind == JsltParserConstants.STRING) LiteralExpression(
            TextNode(
                makeString(
                    ctx,
                    token
                )
            ), loc
        ) else if (kind == JsltParserConstants.TRUE) LiteralExpression(
            BooleanNode.TRUE,
            loc
        ) else if (kind == JsltParserConstants.FALSE) LiteralExpression(
            BooleanNode.FALSE,
            loc
        ) else if (kind == JsltParserConstants.DOT || kind == JsltParserConstants.VARIABLE || kind == JsltParserConstants.IDENT || kind == JsltParserConstants.PIDENT) chainable2Expr(
            ctx,
            getChild(localNode, 0)
        ) else if (kind == JsltParserConstants.IF) {
            var letelse: Array<LetExpression> = emptyArray()
            var theelse: ExpressionNode? = null
            val maybeelse = getLastChild(localNode)
            if (maybeelse!!.jjtGetFirstToken().kind == JsltParserConstants.ELSE) {
                val elseexpr = getLastChild(maybeelse)
                theelse = node2expr(ctx, elseexpr)
                letelse = buildLets(ctx, maybeelse)
            }
            val thenelse = buildLets(ctx, localNode)
            IfExpression(
                node2expr(ctx, localNode.jjtGetChild(0) as SimpleNode),
                thenelse,
                node2expr(
                    ctx,
                    localNode.jjtGetChild(thenelse.size + 1) as SimpleNode
                ),
                letelse,
                theelse,
                loc
            )
        } else if (kind == JsltParserConstants.LBRACKET) {
            val next = token.next
            if (next.kind == JsltParserConstants.FOR) buildForExpression(
                ctx,
                localNode
            ) else ArrayExpression(
                children2Exprs(
                    ctx,
                    localNode
                ), loc
            )
        } else if (kind == JsltParserConstants.LCURLY) {
            val next = token.next
            if (next.kind == JsltParserConstants.FOR) buildObjectComprehension(
                ctx,
                localNode
            ) else buildObject(ctx, localNode)
        } else if (kind == JsltParserConstants.LPAREN) {
            // we don't need a node for the parentheses - so just build the
            // child as a single node and use that instead
            val parens =
                descendTo(localNode, JsltParserTreeConstants.JJTPARENTHESIS)
            node2expr(
                ctx,
                getChild(parens, 0)
            )
        } else {
            localNode.dump(">")
            throw JsltException(
                "INTERNAL ERROR: I'm confused now: " +
                        localNode.jjtGetNumChildren() + " " + kind
            )
        }
    }

    private fun chainable2Expr(ctx: ParseContext, node: SimpleNode): ExpressionNode {
        if (node.id != JsltParserTreeConstants.JJTCHAINABLE) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
        var token = node.jjtGetFirstToken()
        val kind = token.kind
        val loc = makeLocation(ctx, node)

        // need to special-case the first node
        val start: ExpressionNode
        if (kind == JsltParserConstants.VARIABLE) start =
            VariableExpression(
                token.image.substring(1),
                loc
            ) else if (kind == JsltParserConstants.IDENT) {
            val fnode = descendTo(node, JsltParserTreeConstants.JJTFUNCTIONCALL)

            // function or macro call, where the children are the parameters
            val mac = ctx.getMacro(token.image)
            if (mac != null) start = MacroExpression(mac, children2Exprs(ctx, fnode), loc) else {
                // we don't resolve the function here, because it may not have been
                // declared yet. instead we store the name, and do the resolution
                // later
                start = FunctionExpression(
                    token.image, children2Exprs(ctx, fnode), loc
                )
                // remember, so we can resolve later
                ctx.rememberFunctionCall(start)
            }
        } else if (kind == JsltParserConstants.PIDENT) {
            val fnode = descendTo(node, JsltParserTreeConstants.JJTFUNCTIONCALL)

            // imported function must already be there and cannot be a macro
            val pident = token.image
            val colon = pident.indexOf(':') // grammar ensures it's there
            val prefix = pident.substring(0, colon)
            val name = pident.substring(colon + 1)

            // throws exception if something fails
            val c = ctx.getImportedCallable(prefix, name, loc)
            start = if (c is Function) {
                val `fun` = FunctionExpression(
                    pident, children2Exprs(ctx, fnode), loc
                )
                `fun`.resolve(c)
                `fun`
            } else MacroExpression(
                (c as Macro),
                children2Exprs(ctx, fnode),
                loc
            )
        } else if (kind == JsltParserConstants.DOT) {
            token = token.next
            if (token.kind != JsltParserConstants.IDENT && token.kind != JsltParserConstants.STRING && token.kind != JsltParserConstants.LBRACKET)
                return DotExpression(location = loc) // there was only a dot

            // ok, there was a key or array slicer
            start = buildChainLink(ctx, node, null)
        } else throw JsltException("INTERNAL ERROR: Now I'm *really* confused!")

        // then tack on the rest of the chain, if there is any
        return if (node.jjtGetNumChildren() > 0 &&
            getLastChild(node)!!.id == JsltParserTreeConstants.JJTCHAINLINK
        ) buildDotChain(
            ctx,
            getLastChild(node),
            start
        ) else start
    }

    private fun buildDotChain(
        ctx: ParseContext,
        chainLink: SimpleNode?,
        parent: ExpressionNode
    ): ExpressionNode {
        if (chainLink!!.id != JsltParserTreeConstants.JJTCHAINLINK) throw JsltException("INTERNAL ERROR: Wrong type of node: $chainLink")
        var dot = buildChainLink(ctx, chainLink, parent)

        // check if there is more, if so, build
        if (chainLink.jjtGetNumChildren() == 2) dot = buildDotChain(ctx, getChild(chainLink, 1), dot)
        return dot
    }

    private fun buildChainLink(
        ctx: ParseContext,
        node: SimpleNode?,
        parent: ExpressionNode?
    ): ExpressionNode {
        var token = node!!.jjtGetFirstToken()
        return if (token.kind == JsltParserConstants.DOT) {
            // it's a dotkey
            token = token.next // step to token after DOT
            val loc = makeLocation(ctx, node)
            if (token.kind == JsltParserConstants.LBRACKET) return DotExpression(location = loc) // it's .[...]
            val key = identOrString(ctx, token)
            DotExpression(key, parent, loc)
        } else buildArraySlicer(
            ctx,
            getChild(node, 0),
            parent
        )
    }

    private fun buildArraySlicer(
        ctx: ParseContext,
        node: SimpleNode,
        parent: ExpressionNode?
    ): ExpressionNode {
        var colon = false // slicer or index?
        var left: ExpressionNode? = null
        val first = getChild(node, 0)
        if (first.id != JsltParserTreeConstants.JJTCOLON) left = node2expr(ctx, first)
        var right: ExpressionNode? = null
        val last = getLastChild(node)
        if (node.jjtGetNumChildren() != 1 &&
            last!!.id != JsltParserTreeConstants.JJTCOLON
        ) right = node2expr(ctx, last)
        for (ix in 0 until node.jjtGetNumChildren()) colon =
            colon || getChild(node, ix).id == JsltParserTreeConstants.JJTCOLON
        val loc = makeLocation(ctx, node)
        return ArraySlicer(left, colon, right, parent, loc)
    }

    private fun buildForExpression(ctx: ParseContext, node: SimpleNode): ForExpression {
        val valueExpr = node2expr(ctx, getChild(node, 0))
        val lets = buildLets(ctx, node)
        var loopExpr = node2expr(ctx, getLastChild(node))
        var ifExpr: ExpressionNode? = null
        if (node.jjtGetNumChildren() > 2 + lets.size) {
            // there is an if expression, so what we thought was the loopExpr
            // was actually the ifExpr
            ifExpr = loopExpr
            // now get the correct loopExpr
            loopExpr = node2expr(ctx, getChild(node, node.jjtGetNumChildren() - 2))
        }
        return ForExpression(
            valueExpr,
            lets,
            loopExpr,
            ifExpr,
            makeLocation(ctx, node)
        )
    }

    private fun identOrString(ctx: ParseContext, token: Token): String {
        return if (token.kind == JsltParserConstants.STRING) makeString(
            ctx,
            token
        ) else token.image
    }

    private fun makeString(ctx: ParseContext, literal: Token): String {
        // we need to handle escape sequences, so therefore we walk
        // through the entire string, building the output step by step
        val string = literal.image
        val result = CharArray(string.length - 2)
        var pos = 0 // position in result array
        var ix = 1
        while (ix < string.length - 1) {
            var ch = string[ix]
            if (ch != '\\') result[pos++] = ch else {
                ch = string[++ix]
                when (ch) {
                    '\\' -> result[pos++] = ch
                    '"' -> result[pos++] = ch
                    'n' -> result[pos++] = '\n'
                    'b' -> result[pos++] = '\u0008'
                    'f' -> result[pos++] = '\u000C'
                    'r' -> result[pos++] = '\r'
                    't' -> result[pos++] = '\t'
                    '/' -> result[pos++] = '/'
                    'u' -> {
                        if (ix + 5 >= string.length) throw JsltException(
                            "Unfinished Unicode escape sequence",
                            makeLocation(ctx, literal)
                        )
                        result[pos++] = interpretUnicodeEscape(string, ix + 1)
                        ix += 4
                    }
                    else -> throw JsltException(
                        "Unknown escape sequence: \\$ch",
                        makeLocation(ctx, literal)
                    )
                }
            }
            ix++
        }
        return String(result, 0, pos)
    }

    private fun interpretUnicodeEscape(string: String, start: Int): Char {
        var codepoint = 0
        for (ix in 0..3) {
            codepoint = codepoint * 16 + interpretHexDigit(string[start + ix]).code
        }
        return codepoint.toChar()
    }

    private fun interpretHexDigit(digit: Char): Char =
        when (digit) {
            in '0'..'9' -> (digit - '0').toChar()
            in 'A'..'F' -> (digit - 'A' + 10).toChar()
            in 'a'..'f' -> (digit - 'a' + 10).toChar()
            else -> throw JsltException("Bad Unicode escape hex digit: '$digit'")
        }

    private fun children2Exprs(
        ctx: ParseContext,
        node: SimpleNode
    ): Array<ExpressionNode> {
        return (0 until node.jjtGetNumChildren())
            .map { ix -> node2expr(ctx, node.jjtGetChild(ix) as SimpleNode) }
            .toTypedArray()
    }

    // performs all the import directives and register the prefixes
    private fun processImports(ctx: ParseContext, parent: SimpleNode) {
        for (ix in 0 until parent.jjtGetNumChildren()) {
            val node = parent.jjtGetChild(ix) as SimpleNode
            if (node.firstToken.kind != JsltParserConstants.IMPORT) continue
            var token = node.jjtGetFirstToken() // 'import'
            token = token.next // source
            val source = makeString(ctx, token)
            token = token.next // 'as'
            token = token.next // prefix
            val prefix = token.image

            // first check if it's a named module
            val module = ctx.getNamedModule(source)
            if (module != null) ctx.registerModule(prefix, module) else {
                // it's not, so load
                val file = doImport(ctx, source, node, prefix)
                ctx.registerModule(prefix, file)
                ctx.addDeclaredFunction(prefix, file)
                ctx.registerJsltFile(file)
            }
        }
    }

    private fun doImport(
        parent: ParseContext, source: String,
        node: SimpleNode, prefix: String
    ): JsltFile {
        if (parent.isAlreadyImported(source)) throw JsltException(
            "Module '$source' is already imported",
            makeLocation(parent, node)
        )
        val expr = compileImport(parent.extensions, parent, source)
        return JsltFile(prefix, source, expr)
    }

    // collects all the 'let' statements as children of this node
    private fun buildLets(ctx: ParseContext, parent: SimpleNode): Array<LetExpression> {
        val lets = mutableListOf<LetExpression>()
        for (ix in 0 until parent.jjtGetNumChildren()) {
            val node = parent.jjtGetChild(ix) as SimpleNode
            if (node.firstToken.kind != JsltParserConstants.LET) continue
            val loc = makeLocation(ctx, node)
            val ident = node.jjtGetFirstToken().next
            val expr = node.jjtGetChild(0) as SimpleNode
            lets += LetExpression(
                ident.image,
                node2expr(ctx, expr),
                loc
            )
        }
        return lets.toTypedArray()
    }

    // collects all the 'def' statements as children of this node
    // functions are registered with the context
    private fun collectFunctions(ctx: ParseContext, parent: SimpleNode) {
        for (ix in 0 until parent.jjtGetNumChildren()) {
            val node = parent.jjtGetChild(ix) as SimpleNode
            if (node.firstToken.kind != JsltParserConstants.DEF) continue
            val name = node.jjtGetFirstToken().next.image
            val params = collectParams(node)
            val lets = buildLets(ctx, node)
            val func = FunctionDeclaration(
                name, params, lets, node2expr(ctx, getLastChild(node))
            )
            func.computeMatchContexts(null)
            ctx.addDeclaredFunction(name, func)
        }
    }

    private fun collectParams(node: SimpleNode): Array<String> {
        var token = node.jjtGetFirstToken() // DEF
        token = token.next // IDENT
        token = token.next // LPAREN
        val params: MutableList<String> = ArrayList()
        while (token.kind != JsltParserConstants.RPAREN) {
            if (token.kind == JsltParserConstants.IDENT) params.add(token.image)
            token = token.next
        }
        return params.toTypedArray()
    }

    private fun buildObject(ctx: ParseContext, node: SimpleNode): ObjectExpression {
        val lets = buildLets(ctx, node)
        val last = getLastChild(node)
        val matcher = collectMatcher(ctx, last)
        val pairs = collectPairs(ctx, last)
        val children: Array<PairExpression> = pairs.toTypedArray()
        return ObjectExpression(
            lets, children, matcher,
            makeLocation(ctx, node),
            ctx.objectFilter
        )
    }

    private fun collectMatcher(
        ctx: ParseContext,
        node: SimpleNode?
    ): MatcherExpression? {
        if (node == null) return null
        val last = getLastChild(node)
        return if (node.id == JsltParserTreeConstants.JJTPAIR) {
            if (node.jjtGetNumChildren() == 2) null else collectMatcher(
                ctx,
                last
            ) // last in chain was a pair
        } else if (node.id == JsltParserTreeConstants.JJTMATCHER) {
            val minuses: MutableList<String> = ArrayList()
            if (node.jjtGetNumChildren() == 2) // means there was "* - foo : ..."
                collectMinuses(
                    ctx,
                    getChild(node, 0),
                    minuses
                )
            MatcherExpression(
                node2expr(ctx, last), minuses,
                makeLocation(ctx, last!!)
            )
        } else if (node.id == JsltParserTreeConstants.JJTLET) null // last item is a let, which is messed up, but legal
        else throw JsltException("INTERNAL ERROR: This is wrong: $node")
    }

    private fun collectMinuses(
        ctx: ParseContext, node: SimpleNode,
        minuses: MutableList<String>
    ) {
        var token = node.jjtGetFirstToken()
        token = token.next // skip the -
        while (true) {
            minuses.add(identOrString(ctx, token))
            token = token.next
            if (token.kind == JsltParserConstants.COLON) break
            // else: COMMA
            token = token.next
        }
    }

    private fun collectPairs(
        ctx: ParseContext,
        pair: SimpleNode?,
        pairs: MutableList<PairExpression> = ArrayList()
    ): List<PairExpression> {
        return if (pair != null && pair.id == JsltParserTreeConstants.JJTPAIR) {
            val key = node2expr(ctx, pair.jjtGetChild(0) as SimpleNode)
            val value = node2expr(ctx, pair.jjtGetChild(1) as SimpleNode)
            pairs.add(PairExpression(key, value, makeLocation(ctx, pair)))
            if (pair.jjtGetNumChildren() > 1) {
                collectPairs(ctx, getLastChild(pair), pairs)
            }
            pairs
        } else  // has to be a matcher, so we're done
            pairs
    }

    private fun buildObjectComprehension(ctx: ParseContext, node: SimpleNode): ObjectComprehension {
        // children: loop-expr let* key-expr value-expr if-expr?
        val loopExpr = node2expr(ctx, getChild(node, 0))
        val lets = buildLets(ctx, node)
        val ix = lets.size + 1
        val keyExpr = node2expr(ctx, getChild(node, ix))
        val valueExpr = node2expr(ctx, getChild(node, ix + 1))
        var ifExpr: ExpressionNode? = null
        if (node.jjtGetNumChildren() > lets.size + 3) // there is an if
            ifExpr = node2expr(ctx, getLastChild(node))
        return ObjectComprehension(
            loopExpr, lets, keyExpr, valueExpr, ifExpr,
            makeLocation(ctx, node),
            ctx.objectFilter
        )
    }

    private fun getChild(node: SimpleNode, ix: Int): SimpleNode {
        return node.jjtGetChild(ix) as SimpleNode
    }

    private fun getLastChild(node: SimpleNode): SimpleNode? {
        return if (node.jjtGetNumChildren() == 0) null else node.jjtGetChild(node.jjtGetNumChildren() - 1) as SimpleNode
    }

    private fun descendTo(node: SimpleNode, type: Int): SimpleNode {
        return if (node.id == type) node else descendTo(
            node.jjtGetChild(0) as SimpleNode,
            type
        )
    }

    private fun countChildren(node: SimpleNode, type: Int): Int {
        var count = 0
        for (ix in 0 until node.jjtGetNumChildren()) if (getChild(node, ix).id == type) count++
        return count
    }

    private fun makeLocation(ctx: ParseContext, node: SimpleNode): Location {
        val token = node.jjtGetFirstToken()
        return Location(ctx.source, token.beginLine, token.beginColumn)
    }

    private fun makeLocation(ctx: ParseContext, token: Token): Location {
        return Location(ctx.source, token.beginLine, token.beginColumn)
    }
}