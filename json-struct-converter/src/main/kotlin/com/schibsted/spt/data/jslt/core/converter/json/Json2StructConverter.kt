package com.schibsted.spt.data.jslt.core.converter.json

import com.schibsted.spt.data.jslt.core.struct.*
import java.io.EOFException
import java.io.InputStream
import java.text.ParseException

class Json2StructConverter(private val bytes: ByteArray) : StructConverter {
    companion object {
        // we can safely do byte comparison on those characters, as UTF-8 always marks every non-ascii char
        // with the highest bit set in every sub-byte of the encoded character
        const val arrayStartChar = '['.code.toByte()
        const val arrayEndChar = ']'.code.toByte()
        const val objectStartChar = '{'.code.toByte()
        const val objectEndChar = '}'.code.toByte()
        const val stringQuoteChar = '"'.code.toByte()
        const val escapeChar = '\\'.code.toByte()
        const val spaceChar = ' '.code.toByte()
        const val newlineChar = '\n'.code.toByte()
        const val tabChar = '\t'.code.toByte()
        const val backspaceChar = '\u0008'.code.toByte()
        const val feedChar = '\u000C'.code.toByte()
        const val carriageReturnChar = '\r'.code.toByte()
        const val comma = ','.code.toByte()
        const val colon = ':'.code.toByte()
        const val minus = '-'.code.toByte()
        const val plus = '+'.code.toByte()
        const val dot = '.'.code.toByte()
        const val digit0 = '0'.code.toByte()
        const val digit1 = '1'.code.toByte()
        const val digit2 = '2'.code.toByte()
        const val digit3 = '3'.code.toByte()
        const val digit4 = '4'.code.toByte()
        const val digit5 = '5'.code.toByte()
        const val digit6 = '6'.code.toByte()
        const val digit7 = '7'.code.toByte()
        const val digit8 = '8'.code.toByte()
        const val digit9 = '9'.code.toByte()
        const val exponentIdentifierLowerChar = 'e'.code.toByte()
        const val exponentIdentifierUpperChar = 'E'.code.toByte()
        val digits = setOf(
            digit0,
            digit1,
            digit2,
            digit3,
            digit4,
            digit5,
            digit6,
            digit7,
            digit8,
            digit9)
        const val falseFirstChar = 'f'.code.toByte()
        const val trueFirstChar = 't'.code.toByte()
        const val nullUpperFirstChar = 'N'.code.toByte()
        const val nullLowerFirstChar = 'n'.code.toByte()
    }

    private var index = 0
        set(value) {
            field = value
            column++
        }
    private var line = 1
    private var column = 1
    private val size = bytes.size

    constructor(inputStream: InputStream) : this(inputStream.readBytes())
    constructor(string: String) : this(string.toByteArray())

    private fun getPositionString(): String = "index $index (line $line, column $column)"
    private fun getCurrentChar(): Char = bytes[index].toInt().toChar()


    override fun asStruct(): Node {
        index = 0
        line = 1
        column = 1
        val result = parseAnyNode()
        skipWhitespaces()
        if (index < size) {
            throw ParseException("Input contains trailing characters that are not part of the Json parsed Json after ${getPositionString()}.",
                index)
        }
        return result
    }

    private fun parseAnyNode(): Node {
        skipWhitespaces()

        if (index < size) {
            return when (bytes[index]) {
                arrayStartChar -> parseArray()
                objectStartChar -> parseObject()
                stringQuoteChar -> TextNode(parseString())
                minus, dot, digit0, digit1, digit2, digit3, digit4, digit5, digit6, digit7, digit8, digit9 ->
                    parseNumber()
                falseFirstChar, trueFirstChar -> parseBoolean()
                nullUpperFirstChar, nullLowerFirstChar -> parseNull()
                else -> throw ParseException("Invalid character ${getCurrentChar()} at ${getPositionString()}. Expected Array, Object, String, Number, true, false or null.",
                    index)
            }
        } else {
            throw EOFException("Reached end of input but expected any of Array, Object, String, Number, true, false or null.")
        }
    }

    private fun skipWhitespaces() {
        while (index < size &&
            (bytes[index] == spaceChar ||
                    bytes[index] == newlineChar ||
                    bytes[index] == tabChar ||
                    bytes[index] == backspaceChar ||
                    bytes[index] == feedChar ||
                    bytes[index] == carriageReturnChar)
        ) {
            when (bytes[index]) {
                spaceChar, newlineChar, tabChar, backspaceChar, feedChar, carriageReturnChar -> {
                    if (bytes[index] == newlineChar) {
                        line++
                        index++
                        column = 1
                    } else {
                        index++
                    }
                }
            }
        }
    }

    private fun parseNumber(): Node.Number {
        /*
          INTEGER: ("-")? ("0" | ["1"-"9"] (["0"-"9"])*)
          DECIMAL: ("-")? ("0" | ["1"-"9"] (["0"-"9"])*) ("." (["0"-"9"])+ | ("." (["0"-"9"])+)? ("e"|"E") ("+"|"-")? (["0"-"9"])+)
         */
        val sign = if (bytes[index] == minus) "-" else ""
        if (sign.isNotEmpty()) {
            index++
        }
        val intStartIndex = index
        while (index < size && bytes[index] in digits) {
            index++
        }
        val intAsString = bytes.copyOfRange(intStartIndex, index).decodeToString()
        if (intAsString.length > 1 && intAsString.startsWith('0')) {
            throw ParseException("Invalid character at ${getPositionString()}. Integer values should not start with 0.",
                index)
        }
        if (index == size || bytes[index] != dot) {
            return Node.Number.Integral.fromString("$sign$intAsString")
        } else {
            if (index < size && bytes[index] == dot) {
                if (intAsString.isEmpty()) {
                    throw ParseException("Invalid character at ${getPositionString()}. " +
                            "Real number without integer part.", index)
                }
                index++
                if (index < size && bytes[index] !in digits) {
                    throw ParseException("Invalid character at ${getPositionString()}. " +
                            "Expected digit after decimal point.", index)
                }
                val decimalsStartIndex = index
                while (index < size && bytes[index] in digits) {
                    index++
                }
                val decimalsAsString = bytes.copyOfRange(decimalsStartIndex, index).decodeToString()
                if (decimalsAsString.isEmpty()) {
                    throw ParseException("Invalid character at ${getPositionString()}. " +
                            "Number with decimal dot but without decimal part.", index)
                }
                if (index == size || (bytes[index] != exponentIdentifierLowerChar && bytes[index] != exponentIdentifierUpperChar)) {
                    return Node.Number.Decimal.fromString("$sign$intAsString.$decimalsAsString")
                } else {
                    if (index < size && (bytes[index] == exponentIdentifierLowerChar || bytes[index] == exponentIdentifierUpperChar)) {
                        index++
                        if (index < size && (bytes[index] !in digits && bytes[index] != minus && bytes[index] != plus)) {
                            throw ParseException("Invalid character at ${getPositionString()}. Expected digit after exponent.",
                                index)
                        }
                        val exponentStartIndex = index
                        if (index < size && (bytes[index] == minus || bytes[index] == plus)) {
                            index++
                        }
                        while (index < size && bytes[index] in digits) {
                            index++
                        }
                        val exponentAsString = bytes.copyOfRange(exponentStartIndex, index).decodeToString()
                        return Node.Number.Decimal.fromString("$sign$intAsString.$decimalsAsString", exponentAsString)
                    }
                }
            }
        }
        throw ParseException("Unexpected parser state at ${getPositionString()}.", index)
    }

    private fun parseBoolean(): BooleanNode {
        val lenFalse = 5 // False
        val lenTrue = 4 // True

        if (index <= size - lenFalse && bytes[index] == falseFirstChar) {
            val subStr = bytes.copyOfRange(index, index + lenFalse).decodeToString()
            if (subStr == "false") {
                index += lenFalse
                return BooleanNode(false)
            } else {
                throw ParseException("Invalid character at ${getPositionString()}. Expected false but found ${subStr}.",
                    index)
            }
        } else if (index <= size - lenTrue && bytes[index] == trueFirstChar) {
            val subStr = bytes.copyOfRange(index, index + lenTrue).decodeToString()
            if (subStr == "true") {
                index += lenTrue
                return BooleanNode(true)
            } else {
                throw ParseException("Invalid character at ${getPositionString()}. Expected true but found ${subStr}.",
                    index)
            }
        } else {
            throw EOFException("Unexpected EOF. Expected 'true' or 'false' at ${getPositionString()} but input is too short.")
        }
    }

    private fun parseNull(): NullNode {
        val lenNull = 4 // Null

        if (index <= size - lenNull && (bytes[index] == nullUpperFirstChar || bytes[index] == nullLowerFirstChar)) {
            val subStr = bytes.copyOfRange(index, index + lenNull).decodeToString()
            if (subStr.lowercase() == "null") {
                index += lenNull
                return NullNode()
            } else {
                throw ParseException("Invalid character at ${getPositionString()}. Expected null but found ${subStr}.",
                    index)
            }
        } else {
            throw EOFException("Unexpected EOF. Expected 'null' at ${getPositionString()} but input is too short.")
        }
    }

    private fun parseString(): String {
        var lastWasEscapeChar = false
        val startIndex = index + 1 // skip first one, as this is the string enclosing character " or '
        while (index < size) {
            index++
            if (bytes[index] == escapeChar) {
                lastWasEscapeChar = true
            } else if (bytes[index] == newlineChar) {
                throw ParseException("Invalid character at ${getPositionString()}. Linebreaks are not allowed in strings.",
                    index)
            } else if (bytes[index] == stringQuoteChar && !lastWasEscapeChar) {
                val endIndex = index // note: indexTo parameter of copyOfRange is exclusive
                index++ // skip closing " or '
                return if (startIndex < endIndex) {
                    bytes.copyOfRange(startIndex, endIndex).decodeToString()
                } else {
                    ""
                }
            } else {
                if (lastWasEscapeChar && bytes[index].toInt().toChar() !in listOf('n', 't', 'r', '"', '\\')
                ) {
                    throw ParseException("Invalid escaped character '${getCurrentChar()}' at ${getPositionString()}.",
                        index)
                }
                if (!lastWasEscapeChar && bytes[index].toInt() < 32) {
                    throw ParseException("Unescaped control character '${getCurrentChar()}' at ${getPositionString()}.",
                        index)
                }
                lastWasEscapeChar = false
            }
        }
        throw EOFException("Unexpected EOF. Expected string closing character (\") for string at ${getPositionString()} but input is too short.")
    }

    private fun parseObject(): ObjectNode {
        index++
        val items = mutableMapOf<String, Node>()
        // handle empty object
        if (bytes[index] == objectEndChar) {
            index++
            return ObjectNode(items)
        }
        while (index < size) {
            val key = expectObjectKey()
            expectColon()
            val value = parseAnyNode()
            items[key] = value
            if (expectObjectEnd()) return ObjectNode(items)
            expectComma()
        }
        throw ParseException("Invalid character at ${getPositionString()}. Expected ',' or '}' but found ${getCurrentChar()}.",
            index)
    }

    private fun expectObjectKey() = if (index < size) {
        skipWhitespaces()
        if (bytes[index] == stringQuoteChar) {
            parseString()
        } else {
            throw ParseException("Invalid character at ${getPositionString()}. Expected string but found '${getCurrentChar()}'.",
                index)
        }
    } else {
        throw EOFException("Unexpected EOF. Expected string at ${getPositionString()} but input is too short.")
    }

    private fun expectColon() {
        skipWhitespaces()
        if (index < size) {
            if (bytes[index] == colon) {
                index++
            } else {
                throw ParseException("Invalid character at ${getPositionString()}. Expected ':' but found ${getCurrentChar()}.",
                    index)
            }
        } else {
            throw EOFException("Unexpected EOF. Expected colon at ${getPositionString()} but input is too short.")
        }
    }

    private fun expectObjectEnd(): Boolean = expectEnd(objectEndChar)

    private fun parseArray(): ArrayNode {
        index++
        val items = mutableListOf<Node>()
        // handle empty array
        if (bytes[index] == arrayEndChar) {
            index++
            return ArrayNode(items)
        }
        while (index < size) {
            val value = parseAnyNode()
            items.add(value)
            if (expectArrayEnd()) return ArrayNode(items)
            expectComma()
        }
        throw ParseException("Invalid character at ${getPositionString()}. Expected ',' or ']' but found ${getCurrentChar()}.",
            index)
    }

    private fun expectArrayEnd(): Boolean = expectEnd(arrayEndChar)

    private fun expectComma() {
        skipWhitespaces()
        if (index < size) {
            if (bytes[index] == comma) {
                index++
            } else {
                throw ParseException("Invalid character at ${getPositionString()}. Expected ',' but found ${getCurrentChar()}.",
                    index)
            }
        }
    }

    private fun expectEnd(endChar: Byte): Boolean {
        skipWhitespaces()
        if (index < size) {
            return if (bytes[index] == endChar) {
                index++
                true
            } else {
                false
            }
        }
        return false
    }

    override fun toString(): String = bytes.contentToString()
}