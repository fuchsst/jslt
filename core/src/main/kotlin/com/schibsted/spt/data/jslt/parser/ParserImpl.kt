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

import com.fasterxml.jackson.databind.node.*
import com.schibsted.spt.data.jslt.Expression
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.*
import com.schibsted.spt.data.jslt.impl.expressions.*
import com.schibsted.spt.data.jslt.impl.operator.*
import com.schibsted.spt.data.jslt.impl.operator.comparison.*
import com.schibsted.spt.data.jslt.impl.operator.numeric.DivideOperator
import com.schibsted.spt.data.jslt.impl.operator.numeric.MinusOperator
import com.schibsted.spt.data.jslt.impl.operator.numeric.MultiplyOperator
import com.schibsted.spt.data.jslt.impl.operator.numeric.PlusOperator
import java.io.IOException


fun ParseContext.compileExpression(parser: JsltParser): Expression {
    return try {
        parser.Start()
        val expr = compile(parser.jjtree.rootNode() as SimpleNode)
        expr.setGlobalModules(this.files)
        expr
    } catch (e: ParseException) {
        throw JsltException("Parse error: " + e.message, makeLocation(e.currentToken))
    } catch (e: TokenMgrError) {
        throw JsltException("Parse error: " + e.message)
    }
}

private fun compileImport(
    functions: MutableSet<Function>,
    parent: ParseContext,
    jslt: String
): ExpressionImpl {
    try {
        parent.resolver.resolve(jslt).use { reader ->
            val context = ParseContext(
                extensions = functions,
                source = jslt,
                resolver = parent.resolver,
                namedModules = parent.namedModules,
                files = parent.files,
                preparationContext = parent.preparationContext,
                objectFilter = parent.objectFilter
            )
            context.setParent(parent)
            return context.compileModule(JsltParser(reader))
        }
    } catch (e: IOException) {
        throw JsltException("Couldn't read resource $jslt", e)
    }
}

private fun ParseContext.compileModule(parser: JsltParser): ExpressionImpl = try {
    parser.Module()
    compile(parser.jjtree.rootNode() as SimpleNode)
} catch (e: ParseException) {
    throw JsltException("Parse error: " + e.message, this.makeLocation(e.currentToken))
} catch (e: TokenMgrError) {
    throw JsltException("Parse error: " + e.message)
}

private fun ParseContext.compile(root: SimpleNode): ExpressionImpl {
    processImports(root) // registered with context
    val lets = buildLets(root)
    collectFunctions(root) // registered with context
    val expr = root.getLastChild()
    val top: ExpressionNode? = if (expr!!.id == JsltParserTreeConstants.JJTEXPR) node2expr(expr) else null
    resolveFunctions()
    val impl = ExpressionImpl(lets, this.declaredFunctions, top)
    impl.prepare(this.preparationContext)
    return impl.optimize()
}

private fun ParseContext.node2expr(node: SimpleNode?): ExpressionNode {
    if (node!!.id != JsltParserTreeConstants.JJTEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
    var root = node2OrExpr(node.getFirstChild())
    var ix = 0
    while (node.jjtGetNumChildren() > ix * 2 + 1) {
        val child1 = node.getChild(2 + ix * 2)
        val next = node2OrExpr(child1)

        // get the operator
        val loc = makeLocation(node)
        val child2 = node.getChild(1 + ix * 2)
        val comp = child2.jjtGetFirstToken()
        root = if (comp.kind == JsltParserConstants.PIPE)
            PipeOperator(root, next, loc)
        else
            throw JsltException("INTERNAL ERROR: What kind of operator is this?")
        ix += 1
    }
    return root
}

private fun ParseContext.node2OrExpr(node: SimpleNode): ExpressionNode {
    if (node.id != JsltParserTreeConstants.JJTOREXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
    val first = node2AndExpr(node.getFirstChild())
    if (node.jjtGetNumChildren() == 1) // it's just the base
        return first
    val second = node2OrExpr(node.getChild(1))
    return OrOperator(first, second, makeLocation(node))
}

private fun ParseContext.node2AndExpr(node: SimpleNode): ExpressionNode {
    if (node.id != JsltParserTreeConstants.JJTANDEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
    val first = node2CompExpr(node.getFirstChild())
    if (node.jjtGetNumChildren() == 1) // it's just the base
        return first
    val second = node2AndExpr(node.getChild(1))
    return AndOperator(first, second, makeLocation(node))
}

private fun ParseContext.node2CompExpr(node: SimpleNode): ExpressionNode {
    if (node.id != JsltParserTreeConstants.JJTCOMPARATIVEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
    val first = node2AddExpr(node.getFirstChild())
    if (node.jjtGetNumChildren() == 1) // it's just the base
        return first
    val second = node2AddExpr(node.getChild(2))

    // get the comparator
    val loc = makeLocation(node)
    val comp = node.getChild(1).jjtGetFirstToken()
    return when (comp.kind) {
        JsltParserConstants.EQUALS -> EqualsComparison(first, second, loc)
        JsltParserConstants.UNEQUALS -> UnequalsComparison(first, second, loc)
        JsltParserConstants.BIGOREQ -> BiggerOrEqualComparison(first, second, loc)
        JsltParserConstants.BIGGER -> BiggerComparison(first, second, loc)
        JsltParserConstants.SMALLER -> SmallerComparison(first, second, loc)
        JsltParserConstants.SMALLOREQ -> SmallerOrEqualsComparison(first, second, loc)
        else -> throw JsltException("INTERNAL ERROR: What kind of comparison is this? $node")
    }
}

private fun ParseContext.node2AddExpr(node: SimpleNode): ExpressionNode {
    if (node.id != JsltParserTreeConstants.JJTADDITIVEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
    var root = node2MulExpr(node.getFirstChild())
    var ix = 0
    while (node.jjtGetNumChildren() > ix * 2 + 1) {
        val next = node2MulExpr(node.getChild(2 + ix * 2))

        // get the operator
        val loc = makeLocation(node)
        val comp = node.getChild(1 + ix * 2).jjtGetFirstToken()
        root = when (comp.kind) {
            JsltParserConstants.PLUS -> PlusOperator(root, next, loc)
            JsltParserConstants.MINUS -> MinusOperator(root, next, loc)
            else -> throw JsltException("INTERNAL ERROR: What kind of operator is this?")
        }
        ix += 1
    }
    return root
}

private fun ParseContext.node2MulExpr(node: SimpleNode): ExpressionNode {
    if (node.id != JsltParserTreeConstants.JJTMULTIPLICATIVEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
    var root = node2BaseExpr(node.getFirstChild())
    var ix = 0
    while (node.jjtGetNumChildren() > ix * 2 + 1) {
        val child1 = node.getChild(2 + ix * 2)
        val next = node2BaseExpr(child1)

        // get the operator
        val loc = makeLocation(node)
        val child2 = node.getChild(1 + ix * 2)
        val comp = child2.jjtGetFirstToken()
        root = when (comp.kind) {
            JsltParserConstants.STAR -> MultiplyOperator(root, next, loc)
            JsltParserConstants.SLASH -> DivideOperator(root, next, loc)
            else -> throw JsltException("INTERNAL ERROR: What kind of operator is this?")
        }
        ix += 1
    }
    return root
}

private fun ParseContext.node2BaseExpr(node: SimpleNode): ExpressionNode {
    var localNode = node
    if (localNode.id != JsltParserTreeConstants.JJTBASEEXPR) throw JsltException("INTERNAL ERROR: Wrong type of node: $localNode")
    val loc = makeLocation(localNode)
    var token = localNode.jjtGetFirstToken()
    if (token.kind == JsltParserConstants.LBRACKET || token.kind == JsltParserConstants.LCURLY || token.kind == JsltParserConstants.IF) // it's not a token but a production, so we ditch the Expr node
    // and go down to the level below, which holds the actual info
        localNode = localNode.getFirstChild()
    token = localNode.jjtGetFirstToken()
    return when (val kind = token.kind) {
        JsltParserConstants.NULL -> LiteralExpression(NullNode.instance, loc)
        JsltParserConstants.INTEGER -> {
            val number = token.image.toLong()
            val numberObj =
                if (number > Int.MAX_VALUE || number < Int.MIN_VALUE) LongNode(number) else IntNode(number.toInt())
            LiteralExpression(numberObj, loc)
        }
        JsltParserConstants.DECIMAL -> {
            val number = DoubleNode(token.image.toDouble())
            LiteralExpression(number, loc)
        }
        JsltParserConstants.STRING -> LiteralExpression(TextNode(makeString(token)), loc)
        JsltParserConstants.TRUE -> LiteralExpression(BooleanNode.TRUE, loc)
        JsltParserConstants.FALSE -> LiteralExpression(BooleanNode.FALSE, loc)
        JsltParserConstants.DOT, JsltParserConstants.VARIABLE, JsltParserConstants.IDENT, JsltParserConstants.PIDENT -> chainable2Expr(
            localNode.getFirstChild()
        )
        JsltParserConstants.IF -> {
            var letElse: Array<LetExpression> = emptyArray()
            var theElse: ExpressionNode? = null
            val maybeElse = localNode.getLastChild()
            if (maybeElse!!.jjtGetFirstToken().kind == JsltParserConstants.ELSE) {
                val elseExpr = maybeElse.getLastChild()
                theElse = node2expr(elseExpr)
                letElse = buildLets(maybeElse)
            }
            val thenElse = buildLets(localNode)
            IfExpression(
                node2expr(localNode.getFirstChild()),
                thenElse,
                node2expr(localNode.getChild(thenElse.size + 1)),
                letElse,
                theElse,
                loc
            )
        }
        JsltParserConstants.LBRACKET -> {
            val next = token.next
            if (next.kind == JsltParserConstants.FOR)
                buildForExpression(localNode)
            else
                ArrayExpression(children2Exprs(localNode), loc)
        }
        JsltParserConstants.LCURLY -> {
            val next = token.next
            if (next.kind == JsltParserConstants.FOR)
                buildObjectComprehension(localNode)
            else
                buildObject(localNode)
        }
        JsltParserConstants.LPAREN -> {
            // we don't need a node for the parentheses - so just build the
            // child as a single node and use that instead
            val parens = localNode.descendTo(JsltParserTreeConstants.JJTPARENTHESIS)
            node2expr(parens.getFirstChild())
        }
        else -> {
            localNode.dump(">")
            throw JsltException("INTERNAL ERROR: I'm confused now: ${localNode.jjtGetNumChildren()} $kind")
        }
    }
}

private fun ParseContext.chainable2Expr(node: SimpleNode): ExpressionNode {
    if (node.id != JsltParserTreeConstants.JJTCHAINABLE) throw JsltException("INTERNAL ERROR: Wrong type of node: $node")
    var token = node.jjtGetFirstToken()
    val kind = token.kind
    val loc = makeLocation(node)

    // need to special-case the first node
    val start: ExpressionNode
    if (kind == JsltParserConstants.VARIABLE) start =
        VariableExpression(
            token.image.substring(1),
            loc
        ) else if (kind == JsltParserConstants.IDENT) {
        val fnode = node.descendTo(JsltParserTreeConstants.JJTFUNCTIONCALL)

        // function or macro call, where the children are the parameters
        val mac = getMacro(token.image)
        if (mac != null) start = MacroExpression(mac, children2Exprs(fnode), loc) else {
            // we don't resolve the function here, because it may not have been
            // declared yet. instead we store the name, and do the resolution
            // later
            start = FunctionExpression(token.image, children2Exprs(fnode), loc)
            // remember, so we can resolve later
            rememberFunctionCall(start)
        }
    } else if (kind == JsltParserConstants.PIDENT) {
        val fnode = node.descendTo(JsltParserTreeConstants.JJTFUNCTIONCALL)

        // imported function must already be there and cannot be a macro
        val pIdent = token.image
        val colon = pIdent.indexOf(':') // grammar ensures it's there
        val prefix = pIdent.substring(0, colon)
        val name = pIdent.substring(colon + 1)

        // throws exception if something fails
        val c = getImportedCallable(prefix, name, loc)
        start = if (c is Function) {
            FunctionExpression(pIdent, children2Exprs(fnode), loc).apply { resolve(c) }
        } else
            MacroExpression((c as Macro), children2Exprs(fnode), loc)
    } else if (kind == JsltParserConstants.DOT) {
        token = token.next
        if (token.kind != JsltParserConstants.IDENT && token.kind != JsltParserConstants.STRING && token.kind != JsltParserConstants.LBRACKET)
            return DotExpression(location = loc) // there was only a dot

        // ok, there was a key or array slicer
        start = buildChainLink(node, null)
    } else throw JsltException("INTERNAL ERROR: Now I'm *really* confused!")

    // then tack on the rest of the chain, if there is any
    return if (node.jjtGetNumChildren() > 0 && node.getLastChild()!!.id == JsltParserTreeConstants.JJTCHAINLINK)
        buildDotChain(node.getLastChild(), start)
    else
        start
}

private fun ParseContext.buildDotChain(chainLink: SimpleNode?, parent: ExpressionNode): ExpressionNode {
    if (chainLink!!.id != JsltParserTreeConstants.JJTCHAINLINK) throw JsltException("INTERNAL ERROR: Wrong type of node: $chainLink")
    var dot = buildChainLink(chainLink, parent)

    // check if there is more, if so, build
    if (chainLink.jjtGetNumChildren() == 2) dot = buildDotChain(chainLink.getChild(1), dot)
    return dot
}

private fun ParseContext.buildChainLink(node: SimpleNode?, parent: ExpressionNode?): ExpressionNode {
    var token = node!!.jjtGetFirstToken()
    return if (token.kind == JsltParserConstants.DOT) {
        // it's a dotkey
        token = token.next // step to token after DOT
        val loc = makeLocation(node)
        if (token.kind == JsltParserConstants.LBRACKET) return DotExpression(location = loc) // it's .[...]
        val key = identOrString(token)
        DotExpression(key, parent, loc)
    } else
        buildArraySlicer(node.getFirstChild(), parent)
}

private fun ParseContext.buildArraySlicer(node: SimpleNode, parent: ExpressionNode?): ExpressionNode {
    var colon = false // slicer or index?
    val first = node.getFirstChild()
    val last = node.getLastChild()
    val left: ExpressionNode? = if (first.id != JsltParserTreeConstants.JJTCOLON) node2expr(first) else null
    val right: ExpressionNode? =
        if (node.jjtGetNumChildren() != 1 && last!!.id != JsltParserTreeConstants.JJTCOLON) node2expr(last) else null
    (0 until node.jjtGetNumChildren()).forEach { ix ->
        colon = colon || node.getChild(ix).id == JsltParserTreeConstants.JJTCOLON
    }
    val loc = makeLocation(node)
    return ArraySlicer(left, colon, right, parent, loc)
}

private fun ParseContext.buildForExpression(node: SimpleNode): ForExpression {
    val valueExpr = node2expr(node.getFirstChild())
    val lets = buildLets(node)
    var loopExpr = node2expr(node.getLastChild())
    var ifExpr: ExpressionNode? = null
    if (node.jjtGetNumChildren() > 2 + lets.size) {
        // there is an if expression, so what we thought was the loopExpr
        // was actually the ifExpr
        ifExpr = loopExpr
        // now get the correct loopExpr
        loopExpr = node2expr(node.getChild(node.jjtGetNumChildren() - 2))
    }
    return ForExpression(
        valueExpr,
        lets,
        loopExpr,
        ifExpr,
        makeLocation(node)
    )
}

private fun ParseContext.identOrString(token: Token): String =
    if (token.kind == JsltParserConstants.STRING)
        makeString(token)
    else
        token.image

private fun ParseContext.makeString(literal: Token): String {
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
                        makeLocation(literal)
                    )
                    result[pos++] = interpretUnicodeEscape(string, ix + 1)
                    ix += 4
                }
                else -> throw JsltException(
                    "Unknown escape sequence: \\$ch",
                    makeLocation(literal)
                )
            }
        }
        ix++
    }
    return String(result, 0, pos)
}

private fun interpretUnicodeEscape(string: String, start: Int): Char {
    var codepoint = 0
    (0..3).forEach { index ->
        codepoint = codepoint * 16 + interpretHexDigit(string[start + index]).code
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

private fun ParseContext.children2Exprs(
    node: SimpleNode
): Array<ExpressionNode> {
    return (0 until node.jjtGetNumChildren())
        .map { ix -> node2expr(node.getChild(ix)) }
        .toTypedArray()
}

// performs all the import directives and register the prefixes
private fun ParseContext.processImports(parent: SimpleNode) {
    for (ix in 0 until parent.jjtGetNumChildren()) {
        val node = parent.getChild(ix)
        if (node.firstToken.kind != JsltParserConstants.IMPORT) continue
        var token = node.jjtGetFirstToken() // 'import'
        token = token.next // source
        val source = makeString(token)
        token = token.next // 'as'
        token = token.next // prefix
        val prefix = token.image

        // first check if it's a named module
        val module = getNamedModule(source)
        if (module != null) registerModule(prefix, module) else {
            // it's not, so load
            val file = doImport(source, node, prefix)
            registerModule(prefix, file)
            addDeclaredFunction(prefix, file)
            registerJsltFile(file)
        }
    }
}

private fun ParseContext.doImport(source: String, node: SimpleNode, prefix: String): JsltFile {
    if (isAlreadyImported(source)) throw JsltException(
        "Module '$source' is already imported",
        makeLocation(node)
    )
    val expr = compileImport(extensions, this, source)
    return JsltFile(prefix, source, expr)
}

// collects all the 'let' statements as children of this node
private fun ParseContext.buildLets(parent: SimpleNode): Array<LetExpression> {
    val lets = mutableListOf<LetExpression>()
    for (ix in 0 until parent.jjtGetNumChildren()) {
        val node = parent.getChild(ix)
        if (node.firstToken.kind != JsltParserConstants.LET) continue
        val loc = makeLocation(node)
        val ident = node.jjtGetFirstToken().next
        val expr = node.getFirstChild()
        lets += LetExpression(
            ident.image,
            node2expr(expr),
            loc
        )
    }
    return lets.toTypedArray()
}

// collects all the 'def' statements as children of this node
// functions are registered with the context
private fun ParseContext.collectFunctions(parent: SimpleNode) {
    for (ix in 0 until parent.jjtGetNumChildren()) {
        val node = parent.getChild(ix)
        if (node.firstToken.kind != JsltParserConstants.DEF) continue
        val name = node.jjtGetFirstToken().next.image
        val params = node.collectParams()
        val lets = buildLets(node)
        val func = FunctionDeclaration(
            name, params, lets, node2expr(node.getLastChild())
        )
        func.computeMatchContexts(null)
        addDeclaredFunction(name, func)
    }
}

private fun SimpleNode.collectParams(): Array<String> {
    var token = this.jjtGetFirstToken() // DEF
    token = token.next // IDENT
    token = token.next // LPAREN
    val params: MutableList<String> = ArrayList()
    while (token.kind != JsltParserConstants.RPAREN) {
        if (token.kind == JsltParserConstants.IDENT) params.add(token.image)
        token = token.next
    }
    return params.toTypedArray()
}

private fun ParseContext.buildObject(node: SimpleNode): ObjectExpression {
    val lets = buildLets(node)
    val last = node.getLastChild()
    val matcher = collectMatcher(last)
    val pairs = collectPairs(last)
    val children: Array<PairExpression> = pairs.toTypedArray()
    return ObjectExpression(lets, children, matcher, makeLocation(node), objectFilter)
}

private fun ParseContext.collectMatcher(node: SimpleNode?): MatcherExpression? {
    if (node == null) return null
    val last = node.getLastChild()
    return if (node.id == JsltParserTreeConstants.JJTPAIR) {
        if (node.jjtGetNumChildren() == 2)
            null
        else
            collectMatcher(last) // last in chain was a pair
    } else if (node.id == JsltParserTreeConstants.JJTMATCHER) {
        val minuses: MutableList<String> = ArrayList()
        if (node.jjtGetNumChildren() == 2) // means there was "* - foo : ..."
            collectMinuses(node.getFirstChild(), minuses)
        MatcherExpression(node2expr(last), minuses, makeLocation(last!!))
    } else if (node.id == JsltParserTreeConstants.JJTLET)
        null // last item is a let, which is messed up, but legal
    else
        throw JsltException("INTERNAL ERROR: This is wrong: $node")
}

private fun ParseContext.collectMinuses(node: SimpleNode, minuses: MutableList<String>) {
    var token = node.jjtGetFirstToken()
    token = token.next // skip the -
    while (true) {
        minuses.add(identOrString(token))
        token = token.next
        if (token.kind == JsltParserConstants.COLON) break
        // else: COMMA
        token = token.next
    }
}

private fun ParseContext.collectPairs(
    pair: SimpleNode?, pairs: MutableList<PairExpression> = ArrayList()
): List<PairExpression> {
    return if (pair != null && pair.id == JsltParserTreeConstants.JJTPAIR) {
        val key = node2expr(pair.getFirstChild())
        val value = node2expr(pair.getChild(1))
        pairs.add(PairExpression(key, value, makeLocation(pair)))
        if (pair.jjtGetNumChildren() > 1) {
            collectPairs(pair.getLastChild(), pairs)
        }
        pairs
    } else  // has to be a matcher, so we're done
        pairs
}

private fun ParseContext.buildObjectComprehension(node: SimpleNode): ObjectComprehension {
    // children: loop-expr let* key-expr value-expr if-expr?
    val loopExpr = node2expr(node.getFirstChild())
    val lets = buildLets(node)
    val ix = lets.size + 1
    val keyExpr = node2expr(node.getChild(ix))
    val valueExpr = node2expr(node.getChild(ix + 1))
    val ifExpr: ExpressionNode? =
        if (node.jjtGetNumChildren() > lets.size + 3) // there is an if
            node2expr(node.getLastChild())
        else
            null
    return ObjectComprehension(
        loop = loopExpr,
        lets = lets,
        key = keyExpr,
        value = valueExpr,
        ifExpr = ifExpr,
        location = makeLocation(node),
        filter = objectFilter
    )
}

private fun SimpleNode.getChild(index: Int): SimpleNode = this.jjtGetChild(index) as SimpleNode

private fun SimpleNode.getFirstChild(): SimpleNode = this.getChild(0)

private fun SimpleNode.getLastChild(): SimpleNode? =
    if (this.jjtGetNumChildren() == 0)
        null
    else
        this.getChild(this.jjtGetNumChildren() - 1)

private fun SimpleNode.descendTo(jsltParserTreeNodeId: Int): SimpleNode =
    if (this.id == jsltParserTreeNodeId)
        this
    else
        this.getFirstChild().descendTo(jsltParserTreeNodeId)


private fun ParseContext.makeLocation(node: SimpleNode): Location =
    node.jjtGetFirstToken().let { token -> Location(this.source, token.beginLine, token.beginColumn) }


private fun ParseContext.makeLocation(token: Token): Location =
    Location(source, token.beginLine, token.beginColumn)
