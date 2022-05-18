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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.*
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.core.struct.*
import com.schibsted.spt.data.jslt.impl.*
import com.schibsted.spt.data.jslt.impl.operator.comparison.EqualsComparison
import com.schibsted.spt.data.jslt.impl.operator.comparison.compareTo
import com.schibsted.spt.data.jslt.impl.util.*
import com.schibsted.spt.data.jslt.impl.util.Utils.printHexBinary
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong


/**
 * For now contains all the various function implementations. Should
 * probably be broken up into separate files and use annotations to
 * capture a lot of this information instead.
 */
object BuiltinFunctions {
    // this will be replaced with a proper Context. need to figure out
    // relationship between compile-time and run-time context first.
    var functions: MutableMap<String?, Function?> = HashMap<String?, Function?>()
    var macros: MutableMap<String, Macro> = HashMap<String, Macro>()

    // ===== HELPER METHODS
    // shared regexp cache
    private val cache: MutableMap<String?, Pattern?> = BoundedCache(1000)

    @Synchronized
    fun getRegexp(regexp: String): Pattern {
        return cache[regexp] ?: run {
            val p = try {
                Pattern.compile(regexp)
            } catch (e: PatternSyntaxException) {
                throw JsltException("Syntax error in regular expression '$regexp'", e)
            }
            cache[regexp] = p
            p
        }
    }

    private fun copy(
        input: String?,
        buf: CharArray,
        bufix: Int,
        from: Int,
        to: Int,
    ): Int {
        var localBufferIndex = bufix
        for (ix in from until to) buf[localBufferIndex++] = input!![ix]
        return localBufferIndex
    }

    abstract class AbstractMacro(name: String?, min: Int, max: Int) : AbstractCallable(name!!, min, max), Macro

    // ===== NUMBER
    class Number : AbstractFunction("number", 1, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return if (arguments.size == 1)
                number(arguments[0], true, null)
            else
                number(arguments[0], false, null, arguments[1])
        }
    }

    // ===== ROUND
    class Round : AbstractFunction("round", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val number = arguments[0]
            if (number.isNull)
                return NullNode.instance
            else if (!number.isNumber)
                throw JsltException("round() cannot round a non-number: $number")
            return LongNode(number.doubleValue().roundToLong())
        }
    }

    // ===== FLOOR
    class Floor : AbstractFunction("floor", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val number = arguments[0]
            if (number.isNull)
                return NullNode.instance
            else if (!number.isNumber)
                throw JsltException("floor() cannot round a non-number: $number")
            return LongNode(floor(number.doubleValue()).toLong())
        }
    }

    // ===== CEILING
    class Ceiling : AbstractFunction("ceiling", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val number = arguments[0]
            if (number.isNull)
                return NullNode.instance
            else if (!number.isNumber)
                throw JsltException("ceiling() cannot round a non-number: $number")
            return LongNode(ceil(number.doubleValue()).toLong())
        }
    }

    // ===== RANDOM
    class Random : AbstractFunction("random", 0, 0) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node =
            DoubleNode(random.nextDouble())

        companion object {
            private val random = java.util.Random()
        }
    }

    // ===== SUM
    class Sum : AbstractFunction("sum", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val array = arguments[0]
            if (array.isNull)
                return NullNode.instance
            else if (!array.isArray)
                throw JsltException("sum(): argument must be array, was $array")
            var sum = 0.0
            var integral = true
            for (ix in 0 until array.size()) {
                val value = array[ix]
                if (!value.isNumber) throw JsltException("sum(): array must contain numbers, found $value")
                integral = integral and value.isIntegralNumber
                sum += value.doubleValue()
            }
            return if (integral) LongNode(sum.toLong()) else DoubleNode(sum)
        }
    }

    // ===== MODULO
    class Modulo : AbstractFunction("modulo", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val dividend = arguments[0]
            if (dividend.isNull) return NullNode.instance else if (!dividend.isNumber) throw JsltException("mod(): dividend cannot be a non-number: $dividend")
            val divisor = arguments[1]
            if (divisor.isNull) return NullNode.instance else if (!divisor.isNumber) throw JsltException("mod(): divisor cannot be a non-number: $divisor")
            return if (!dividend.isIntegralNumber || !divisor.isIntegralNumber) {
                throw JsltException("mod(): operands must be integral types")
            } else {
                val longDivident = dividend.longValue()
                val longDivisor = divisor.longValue()
                if (longDivisor == 0L) throw JsltException("mod(): cannot divide by zero")
                var r = longDivident % longDivisor
                if (r < 0) {
                    if (longDivisor > 0) r += longDivisor else r -= longDivisor
                }
                LongNode(r)
            }
        }
    }

    // ===== HASH-INT
    class HashInt : AbstractFunction("hash-int", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val node = arguments[0]
            return if (node.isNull)
                NullNode.instance
            else
                try {
                    // https://stackoverflow.com/a/18993481/90580
                    val obj = mapper.treeToValue(node, kotlin.Any::class.java)
                    val jsonString = writer.writeValueAsString(obj)
                    IntNode(jsonString.hashCode())
                } catch (e: JsonProcessingException) {
                    throw JsltException("hash-int: can't process json$e")
                }
        }

        companion object {
            private val mapper = JsonMapper.builder()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build()
            private val writer = mapper.writer()
        }
    }

    // ===== TEST
    class Test : AbstractRegexpFunction("test", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            // if data is missing then it doesn't match, end of story
            if (arguments[0].isNull) return BooleanNode.FALSE
            val string = arguments[0].asString()
            val regexp = arguments[1].asString(JsltException("test() can't test null regexp"))
            val p = getRegexp(regexp)
            val m = p.matcher(string)
            return m.find(0).toNode()
        }
    }

    // ===== CAPTURE
    // believe it or not, but the built-in Java regex library is so
    // incredibly shitty that it doesn't allow you to learn what the
    // names of the named groups are. so we have to use regexps to
    // parse the regexps. (lots of swearing omitted.)
    class Capture : AbstractRegexpFunction("capture", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            // if data is missing then it doesn't match, end of story
            if (arguments[0].isNull) return arguments[0] // null
            val string = arguments[0].asString()
            val regexp = arguments[1].asString(JsltException("capture() can't match against null regexp"))

            var regex = cache[regexp]
            if (regex == null) {
                regex = JstlPattern(regexp)
                cache[regexp] = regex
            }
            val node = objectMapper.createObjectNode()
            val m = regex.matcher(string)
            if (m.find()) {
                for (group in regex.groups) {
                    try {
                        node.put(group, m.group(group))
                    } catch (e: IllegalStateException) {
                        // this group had no match: do nothing
                    }
                }
            }
            return node
        }

        companion object {
            var cache: MutableMap<String, JstlPattern?> = BoundedCache(1000)
        }
    }

    // from https://stackoverflow.com/a/15588989/5974641
    class JstlPattern(regexp: String) {
        private val pattern: Pattern = Pattern.compile(regexp)
        val groups: Set<String> = getNamedGroups(regexp)

        fun matcher(input: String?): Matcher {
            return pattern.matcher(input)
        }

        companion object {
            private val extractor = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>")
            private fun getNamedGroups(regex: String): Set<String> {
                val groups: MutableSet<String> = TreeSet()
                val m = extractor.matcher(regex)
                while (m.find()) groups.add(m.group(1))
                return groups
            }
        }

    }

    // ===== SPLIT
    abstract class AbstractRegexpFunction internal constructor(name: String?, min: Int, max: Int) : AbstractFunction(
        name!!, min, max
    ), RegexpFunction {
        override fun regexpArgumentNumber(): Int {
            return 1
        }
    }

    class Split : AbstractRegexpFunction("split", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull) return arguments[0] // null
            val string = arguments[0].asString()
            val split = arguments[1].asString(JsltException("split() can't split on null"))

            return toNode(string.split(split).toTypedArray())
        }
    }

    // ===== LOWERCASE
    class Lowercase : AbstractFunction("lowercase", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull) return arguments[0] // null
            val string = arguments[0].asString()
            return TextNode(string.lowercase(Locale.getDefault()))
        }
    }

    // ===== UPPERCASE
    class Uppercase : AbstractFunction("uppercase", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull) return arguments[0] // null
            val string = arguments[0].asString()
            return TextNode(string.uppercase(Locale.getDefault()))
        }
    }

    // ===== SHA256
    class Sha256 : AbstractFunction("sha256-hex", 1, 1) {
        private val messageDigest: MessageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw JsltException("sha256-hex: could not find sha256 algorithm $e")
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull) return arguments[0] // null
            val message = arguments[0].asString()
            val bytes = messageDigest.digest(message.toByteArray(StandardCharsets.UTF_8))
            val string = printHexBinary(bytes)
            return TextNode(string)
        }
    }

    // ===== NOT
    class Not : AbstractFunction("not", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return (!arguments[0].isTrue()).toNode()
        }
    }

    // ===== BOOLEAN
    class Boolean : AbstractFunction("boolean", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isTrue().toNode()
        }
    }

    // ===== IS-BOOLEAN
    class IsBoolean : AbstractFunction("is-boolean", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isBoolean.toNode()
        }
    }

    // ===== FALLBACK
    class Fallback : AbstractMacro("fallback", 2, 1024) {
        override fun call(
            scope: Scope, input: Node,
            parameters: kotlin.Array<ExpressionNode>,
        ): Node {
            // making this a macro means we can evaluate only the parameters
            // that are necessary to find a value, and leave the rest
            // untouched, giving better performance
            for (ix in parameters.indices) {
                val value = parameters[ix].apply(scope, input)
                if (isValue(value)) return value
            }
            return NullNode.instance
        }
    }

    // ===== IS-OBJECT
    class IsObject : AbstractFunction("is-object", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isObject.toNode()
        }
    }

    // ===== GET-KEY
    class GetKey : AbstractFunction("get-key", 2, 3) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val key = arguments[1].asNullableString() ?: return NullNode.instance
            val obj = arguments[0]
            return when {
                obj.isObject -> {
                    val value = obj[key]
                    value ?: if (arguments.size == 2) NullNode.instance else arguments[2] // fallback argument
                }
                obj.isNull -> NullNode.instance
                else -> throw JsltException("get-key: can't look up keys in $obj")
            }
        }
    }

    // ===== IS-ARRAY
    class IsArray : AbstractFunction("is-array", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isArray.toNode()
        }
    }

    // ===== ARRAY
    class Array : AbstractFunction("array", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val value = arguments[0]
            return if (value.isNull || value.isArray)
                value
            else if (value.isObject)
                value.convertObjectToArray()
            else
                throw JsltException("array() cannot convert $value")
        }
    }

    // ===== FLATTEN
    class Flatten : AbstractFunction("flatten", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val value = arguments[0]
            if (value.isNull) return value else if (!value.isArray) throw JsltException("flatten() cannot operate on $value")
            val array = objectMapper.createArrayNode()
            flatten(array, value)
            return array
        }

        private fun flatten(array: ArrayNode, current: Node) {
            for (ix in 0 until current.size()) {
                val node = current[ix]
                if (node.isArray) flatten(array, node) else array.add(node)
            }
        }
    }

    // ===== ALL
    class All : AbstractFunction("all", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val value = arguments[0]
            if (value.isNull) return value else if (!value.isArray) throw JsltException("all() requires an array, not $value")
            for (ix in 0 until value.size()) {
                val node = value[ix]
                if (!node.isTrue()) return BooleanNode.FALSE
            }
            return BooleanNode.TRUE
        }
    }

    // ===== ANY
    class Any : AbstractFunction("any", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val value = arguments[0]
            if (value.isNull) return value else if (!value.isArray) throw JsltException("any() requires an array, not $value")
            for (ix in 0 until value.size()) {
                if (value[ix].isTrue()) return BooleanNode.TRUE
            }
            return BooleanNode.FALSE
        }
    }

    // ===== ZIP
    class Zip : AbstractFunction("zip", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val array1 = arguments[0]
            val array2 = arguments[1]
            if (array1.isNull || array2.isNull) return NullNode.instance else if (!array1.isArray || !array2.isArray) throw JsltException(
                "zip() requires arrays"
            ) else if (array1.size() != array2.size()) throw JsltException("zip() arrays were of unequal size")
            val array = objectMapper.createArrayNode()
            for (ix in 0 until array1.size()) {
                val pair = objectMapper.createArrayNode()
                pair.add(array1[ix])
                pair.add(array2[ix])
                array.add(pair)
            }
            return array
        }
    }

    // ===== ZIP-WITH-INDEX
    class ZipWithIndex : AbstractFunction("zip-with-index", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val arrayIn = arguments[0]
            if (arrayIn.isNull) return NullNode.instance else if (!arrayIn.isArray) throw JsltException("zip-with-index() argument must be an array")
            val arrayOut = objectMapper.createArrayNode()
            for (ix in 0 until arrayIn.size()) {
                val pair = objectMapper.createObjectNode()
                pair.replace("index", IntNode(ix))
                pair.replace("value", arrayIn[ix])
                arrayOut.add(pair)
            }
            return arrayOut
        }
    }

    // ===== INDEX-OF
    class IndexOf : AbstractFunction("index-of", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val array = arguments[0]
            if (array.isNull) return NullNode.instance else if (!array.isArray) throw JsltException("index-of() first argument must be an array")
            val value = arguments[1]
            for (ix in 0 until array.size()) {
                if (EqualsComparison.equals(array[ix], value)) return IntNode(ix)
            }
            return IntNode(-1)
        }
    }

    // ===== STARTS-WITH
    class StartsWith : AbstractFunction("starts-with", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val string = arguments[0].asNullableString() ?: return BooleanNode.FALSE
            val prefix = arguments[1].asString()
            return string.startsWith(prefix).toNode()
        }
    }

    // ===== ENDS-WITH
    class EndsWith : AbstractFunction("ends-with", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val string = arguments[0].asNullableString() ?: return BooleanNode.FALSE
            val suffix = arguments[1].asString()
            return string.endsWith(suffix).toNode()
        }
    }

    // ===== FROM-JSON
    class FromJson : AbstractFunction("from-json", 1, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val json = arguments[0].asNullableString() ?: return NullNode.instance
            return try {
                objectMapper.readTree(json) ?: // if input is "", for example
                return NullNode.instance
            } catch (e: Exception) {
                if (arguments.size == 2) arguments[1] // return fallback on parse fail
                else throw JsltException("from-json can't parse $json: $e")
            }
        }
    }

    // ===== TO-JSON
    class ToJson : AbstractFunction("to-json", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return try {
                val json = objectMapper.writeValueAsString(arguments[0])
                TextNode(json)
            } catch (e: Exception) {
                throw JsltException("to-json can't serialize " + arguments[0] + ": " + e)
            }
        }
    }

    // ===== REPLACE
    class Replace : AbstractRegexpFunction("replace", 3, 3) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val string = arguments[0].asNullableString() ?: return NullNode.instance
            val regexp = arguments[1].asString()
            val sep = arguments[2].asString()
            val p = getRegexp(regexp)
            val m = p.matcher(string)
            val buf = CharArray(string.length * sep.length.coerceAtLeast(1))
            var pos = 0 // next untouched character in input
            var bufix = 0 // next unwritten character in buf
            while (m.find(pos)) {
                // we found another match, and now matcher state has been updated
                if (m.start() == m.end()) throw JsltException("Regexp " + regexp + " in replace() matched empty string in '" + arguments[0] + "'")

                // if there was text between pos and start of match, copy to output
                if (pos < m.start()) bufix = copy(string, buf, bufix, pos, m.start())

                // copy sep to output (corresponds with the match)
                bufix = copy(sep, buf, bufix, 0, sep.length)

                // step over match
                pos = m.end()
            }
            if (pos == 0 && arguments[0].isTextual) // there were matches, so the string hasn't changed
                return arguments[0] else if (pos < string.length) // there was text remaining after the end of the last match. must copy
                bufix = copy(string, buf, bufix, pos, string.length)
            return TextNode(String(buf, 0, bufix))
        }
    }

    // ===== TRIM
    class Trim : AbstractFunction("trim", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val string = arguments[0].asNullableString() ?: return NullNode.instance
            return TextNode(string.trim { it <= ' ' })
        }
    }

    // ===== UUID

    // ===== UUID
    class Uuid : AbstractFunction("uuid", 0, 2) {
        private fun maskMSB(number: Long): Long {
            val version = (1 shl 12).toLong()
            val least12SignificantBit = number and 0x000000000000FFFFL shr 4
            return (number and -0x10000L) + version + least12SignificantBit
        }

        private fun maskLSB(number: Long): Long {
            val LSB_MASK = 0x3FFFFFFFFFFFFFFFL
            val LSB_VARIANT3_BITFLAG = 0x8000000000000000UL.toLong()
            return (number and LSB_MASK) + LSB_VARIANT3_BITFLAG
        }

        override fun call(input: Node, arguments: kotlin.Array<Node>): Node? {
            val uuid: String = if (arguments.isEmpty()) {
                UUID.randomUUID().toString()
            } else if (arguments.size == 2) {
                // NIL UUID is a special case defined in 4.1.7 of the RFC (https://www.ietf.org/rfc/rfc4122.txt)
                if (arguments[0].isNull && arguments[1].isNull) {
                    "00000000-0000-0000-0000-000000000000"
                } else {
                    val msb: Long = arguments[0].longValue()
                    val lsb: Long = arguments[1].longValue()
                    UUID(maskMSB(msb), maskLSB(lsb)).toString()
                }
            } else {
                throw JsltException("Build-in UUID function must be called with either none or two parameters.")
            }
            return TextNode(uuid)
        }
    }

    // ===== JOIN
    class Join : AbstractFunction("join", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val array = toArray(arguments[0], true) ?: return NullNode.instance
            val sep = arguments[1].asString()
            val buf = StringBuilder()
            for (ix in 0 until array.size()) {
                if (ix > 0) buf.append(sep)
                buf.append(array[ix].asString())
            }
            return TextNode(buf.toString())
        }
    }

    // ===== CONTAINS
    class Contains : AbstractFunction("contains", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            when {
                arguments[1].isNull -> return BooleanNode.FALSE // nothing is contained in null
                arguments[1].isArray -> {
                    for (ix in 0 until arguments[1].size()) if (arguments[1][ix] == arguments[0]) return BooleanNode.TRUE
                }
                arguments[1].isObject -> {
                    val key = arguments[0].asNullableString() ?: return BooleanNode.FALSE
                    return arguments[1].has(key).toNode()
                }
                arguments[1].isTextual -> {
                    val sub = arguments[0].asNullableString() ?: return BooleanNode.FALSE
                    val str = arguments[1].asText()
                    return (str.indexOf(sub) != -1).toNode()
                }
                else -> throw JsltException("Contains cannot operate on " + arguments[1])
            }
            return BooleanNode.FALSE
        }
    }

    // ===== SIZE
    class Size : AbstractFunction("size", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return if (arguments[0].isArray || arguments[0].isObject) IntNode(arguments[0].size()) else if (arguments[0].isTextual) IntNode(
                arguments[0].asText().length
            ) else if (arguments[0].isNull) arguments[0] else throw JsltException(
                "Function size() cannot work on " + arguments[0]
            )
        }
    }

    // ===== ERROR
    class Error : AbstractFunction("error", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val msg = arguments[0].asString()
            throw JsltException("error: $msg")
        }
    }

    // ===== STRING
    class ToString : AbstractFunction("string", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return if (arguments[0].isTextual) arguments[0] else TextNode(arguments[0].toString())
        }
    }

    // ===== IS-STRING
    class IsString : AbstractFunction("is-string", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isTextual.toNode()
        }
    }

    // ===== IS-NUMBER
    class IsNumber : AbstractFunction("is-number", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isNumber.toNode()
        }
    }

    // ===== IS-INTEGER
    class IsInteger : AbstractFunction("is-integer", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isIntegralNumber.toNode()
        }
    }

    // ===== IS-DECIMAL
    class IsDecimal : AbstractFunction("is-decimal", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return arguments[0].isFloatingPointNumber.toNode()
        }
    }

    // ===== NOW
    class Now : AbstractFunction("now", 0, 0) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val ms = System.currentTimeMillis()
            return (ms / 1000.0).toNode()
        }
    }

    // ===== PARSE-TIME
    class ParseTime : AbstractFunction("parse-time", 2, 3) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val text = arguments[0].asNullableString() ?: return NullNode.instance
            val formatStr = arguments[1].asString()
            var fallback: Node? = null
            if (arguments.size > 2) fallback = arguments[2]

            // the performance of this could be better, but it's not so easy
            // to fix that when SimpleDateFormat isn't thread-safe, so we
            // can't safely share it between threads
            return try {
                val format = SimpleDateFormat(formatStr)
                format.timeZone = SimpleTimeZone(0, "UTC")
                val time = format.parse(text)
                (time.time / 1000.0).toNode()
            } catch (e: IllegalArgumentException) {
                // thrown if format is bad
                throw JsltException("parse-time: Couldn't parse format '$formatStr': ${e.message}")
            } catch (e: ParseException) {
                fallback ?: throw JsltException("parse-time: ${e.message}")
            }
        }
    }

    // ===== FORMAT-TIME
    class FormatTime : AbstractFunction("format-time", 2, 3) {
        companion object {
            val zonenames: Set<String?> = TimeZone.getAvailableIDs().toSet()
        }

        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            val number = number(arguments[0], null)
            if (number.isNull) return NullNode.instance
            val timestamp = number.asDouble()
            val formatStr = arguments[1].asString()
            var zone: TimeZone? = SimpleTimeZone(0, "UTC")
            if (arguments.size == 3) {
                val zonename = arguments[2].asString()
                if (!zonenames.contains(zonename)) throw JsltException("format-time: Unknown timezone $zonename")
                zone = TimeZone.getTimeZone(zonename)
            }

            // the performance of this could be better, but it's not so easy
            // to fix that when SimpleDateFormat isn't thread-safe, so we
            // can't safely share it between threads
            return try {
                val format = SimpleDateFormat(formatStr)
                format.timeZone = zone
                val formatted = format.format((timestamp * 1000).roundToLong())
                TextNode(formatted)
            } catch (e: IllegalArgumentException) {
                // thrown if format is bad
                throw JsltException("format-time: Couldn't parse format '" + formatStr + "': " + e.message)
            }
        }
    }

    // ===== MIN
    class Min : AbstractFunction("min", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            // this works because null is the smallest of all values
            return if (arguments[0] < arguments[1])
                arguments[0] else arguments[1]
        }
    }

    // ===== MAX
    class Max : AbstractFunction("max", 2, 2) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            return if (arguments[0].isNull || arguments[1].isNull)
                NullNode.instance
            else if (arguments[0] > arguments[1])
                arguments[0]
            else
                arguments[1]
        }
    }

    // ===== PARSE-URL
    class ParseUrl : AbstractFunction("parse-url", 1, 1) {
        override fun call(input: Node, arguments: kotlin.Array<Node>): Node {
            if (arguments[0].isNull) return NullNode.instance
            val urlString = arguments[0].asText()
            return try {
                val aURL = URL(arguments[0].asText())
                val objectNode = objectMapper.createObjectNode()
                if (aURL.host != null && aURL.host.isNotEmpty()) objectNode.put("host", aURL.host)
                if (aURL.port != -1) objectNode.put("port", aURL.port)
                if (aURL.path.isNotEmpty()) objectNode.put("path", aURL.path)
                if (aURL.protocol != null && aURL.protocol.isNotEmpty()) objectNode.put("scheme", aURL.protocol)
                if (aURL.query != null && aURL.query.isNotEmpty()) {
                    objectNode.put("query", aURL.query)
                    val queryParamsNode = objectMapper.createObjectNode()
                    objectNode.set<Node>("parameters", queryParamsNode)
                    val pairs = aURL.query.split("&").toTypedArray()
                    for (pair in pairs) {
                        val idx = pair.indexOf("=")
                        val key = if (idx > 0) URLDecoder.decode(pair.substring(0, idx), "UTF-8") else pair
                        if (!queryParamsNode.has(key)) queryParamsNode.set<Node>(
                            key,
                            objectMapper.createArrayNode()
                        )
                        val value = if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(
                            pair.substring(idx + 1),
                            "UTF-8"
                        ) else null
                        val valuesNode = queryParamsNode[key] as ArrayNode
                        valuesNode.add(value)
                    }
                }
                if (aURL.ref != null) objectNode.put("fragment", aURL.ref)
                if (aURL.userInfo != null && aURL.userInfo.isNotEmpty()) objectNode.put("userinfo", aURL.userInfo)
                objectNode
            } catch (e: MalformedURLException) {
                throw JsltException("Can't parse $urlString", e)
            } catch (e: UnsupportedEncodingException) {
                throw JsltException("Can't parse $urlString", e)
            }
        }
    }

    init {
        // GENERAL
        functions["contains"] = Contains()
        functions["size"] = Size()
        functions["error"] = Error()
        functions["min"] = Min()
        functions["max"] = Max()

        // NUMERIC
        functions["is-number"] = IsNumber()
        functions["is-integer"] = IsInteger()
        functions["is-decimal"] = IsDecimal()
        functions["number"] = Number()
        functions["round"] = Round()
        functions["floor"] = Floor()
        functions["ceiling"] = Ceiling()
        functions["random"] = Random()
        functions["sum"] = Sum()
        functions["mod"] = Modulo()
        functions["hash-int"] = HashInt()

        // STRING
        functions["is-string"] = IsString()
        functions["string"] = ToString()
        functions["test"] = Test()
        functions["capture"] = Capture()
        functions["split"] = Split()
        functions["join"] = Join()
        functions["lowercase"] = Lowercase()
        functions["uppercase"] = Uppercase()
        functions["sha256-hex"] = Sha256()
        functions["starts-with"] = StartsWith()
        functions["ends-with"] = EndsWith()
        functions["from-json"] = FromJson()
        functions["to-json"] = ToJson()
        functions["replace"] = Replace()
        functions["trim"] = Trim()
        functions["uuid"] = Uuid()

        // BOOLEAN
        functions["not"] = Not()
        functions["boolean"] = Boolean()
        functions["is-boolean"] = IsBoolean()

        // OBJECT
        functions["is-object"] = IsObject()
        functions["get-key"] = GetKey()

        // ARRAY
        functions["array"] = Array()
        functions["is-array"] = IsArray()
        functions["flatten"] = Flatten()
        functions["all"] = All()
        functions["any"] = Any()
        functions["zip"] = Zip()
        functions["zip-with-index"] = ZipWithIndex()
        functions["index-of"] = IndexOf()

        // TIME
        functions["now"] = Now()
        functions["parse-time"] = ParseTime()
        functions["format-time"] = FormatTime()

        // MISC
        functions["parse-url"] = ParseUrl()
    }

    init {
        macros["fallback"] = Fallback()
    }
}