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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.*
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.Scope
import com.schibsted.spt.data.jslt.impl.expressions.LetExpression
import java.math.BigInteger
import kotlin.math.pow


val objectMapper = ObjectMapper()

fun evalLets(scope: Scope, input: JsonNode, lets: Array<LetExpression>?) {
    if (lets == null) return
    lets.forEach { let ->
        // val `var` = lets[ix].variable
        val value = let.apply(scope, input)
        scope.setValue(let.slot, value)
    }
}

fun JsonNode.isTrue(): Boolean =
    this !== BooleanNode.FALSE &&
            !(isObject && size() == 0) &&
            !(isTextual && asText().isEmpty()) &&
            !(isArray && size() == 0) &&
            !(isNumber && doubleValue() == 0.0) &&
            !isNull

fun isValue(value: JsonNode): Boolean = !(value.isNull || !value.isValueNode && value.isEmpty)

fun Boolean.toJsonNode(): JsonNode = if (this) BooleanNode.TRUE else BooleanNode.FALSE

fun Double.toJsonNode(): JsonNode = DoubleNode(this)

fun toJsonNode(array: Array<String?>): JsonNode {
    val elements = array.map { if (it == null) NullNode.instance else TextNode(it) }
    return objectMapper.createArrayNode().addAll(elements)
}


fun JsonNode.asString(exceptionWhenNull: Exception = NullPointerException("JsonNode should not be NULL")): String {
    if (isNull) throw exceptionWhenNull
    // check what type this is
    return if (isTextual) return asText() else toString()
}

fun JsonNode.asNullableString(): String? =
    if (isTextual) // check what type this is
        asText()
    else if (!isNull)
        toString()
    else
        null


fun toArray(value: JsonNode, nullok: Boolean): ArrayNode? {
    // check what type this is
    if (value.isArray) return value as ArrayNode else if (value.isNull && nullok) return null
    throw JsltException("Cannot convert $value to array")
}

fun number(value: JsonNode, loc: Location?=null): JsonNode {
    return number(value, false, loc)
}

@JvmOverloads
fun number(
    value: JsonNode, strict: Boolean, loc: Location?,
    fallback: JsonNode? = null
): JsonNode {
    // check what type this is
    if (value.isNumber) return value else if (value.isNull) {
        return fallback ?: value
    } else if (!value.isTextual) {
        return if (strict) throw JsltException(
            "Can't convert $value to number",
            loc
        ) else fallback ?: NullNode.instance
    }

    // let's look at this number
    val number = value.asText()
    val numberNode = parseNumber(number)
    return if (numberNode == null || !numberNode.isNumber) {
        fallback ?: throw JsltException("number($number) failed: not a number", loc)
    } else {
        numberNode
    }
}

// returns null in case of failure (caller then handles fallback)
private fun parseNumber(number: String): JsonNode? {
    if (number.isEmpty()) return null
    var pos = 0
    if (number[0] == '-') {
        pos = 1
    }
    val endInteger = scanDigits(number, pos)
    if (endInteger == pos) return null
    if (endInteger == number.length) {
        return when {
            number.length < 10 -> IntNode(number.toInt())
            number.length < 19 -> LongNode(number.toLong())
            else -> BigIntegerNode(BigInteger(number))
        }
    }

    // since there's stuff after the initial integer it must be either
    // the decimal part or the exponent
    val intPart = number.substring(0, endInteger).toLong()
    pos = endInteger
    var value = intPart.toDouble()
    if (number[pos] == '.') {
        pos += 1
        val endDecimal = scanDigits(number, pos)
        if (endDecimal == pos) return null
        var decimalPart = number.substring(endInteger + 1, endDecimal).toLong()
        val digits = endDecimal - endInteger - 1

        // if intPart is negative we can't add a positive decimalPart to it
        if (intPart < 0) decimalPart *= -1
        value = decimalPart / 10.0.pow(digits.toDouble()) + intPart
        pos = endDecimal

        // if there's nothing more, then this is it
        if (pos == number.length) return DoubleNode(value)
    }

    // there is more: next character MUST be 'e' or 'E'
    var ch = number[pos]
    if (ch != 'e' && ch != 'E') return null

    // now we must have either '-', '+', or an integer
    pos++
    if (pos == number.length) return null
    ch = number[pos]
    var sign = 1
    if (ch == '+') pos++ else if (ch == '-') {
        sign = -1
        pos++
    }
    val endExponent = scanDigits(number, pos)
    if (endExponent != number.length || endExponent == pos) return null
    val exponent = number.substring(pos).toInt() * sign
    return DoubleNode(value * 10.0.pow(exponent.toDouble()))
}

private fun scanDigits(number: String, pos: Int): Int {
    var currentPos = pos
    while (currentPos < number.length && number[currentPos].isDigit()) currentPos++
    return currentPos
}

fun JsonNode.convertObjectToArray(): ArrayNode {
    val elements = fields().asSequence()
        .map { (key, value) ->
            objectMapper.createObjectNode().apply {
                set<JsonNode>("key", TextNode(key))
                set<JsonNode>("value", value)
            }
        }.toList()
    return objectMapper.createArrayNode().apply {
        addAll(elements)
    }
}

fun indent(level: Int): String = " ".repeat(level * 2)

