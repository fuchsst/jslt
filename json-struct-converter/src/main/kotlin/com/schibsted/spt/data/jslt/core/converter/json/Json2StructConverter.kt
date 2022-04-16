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
        const val commentStartChar = '/'.code.toByte()
        const val multilineCommentSecondStartChar = '*'.code.toByte()
        const val doubleQuoteStringStartChar = '"'.code.toByte()
        const val singleQuoteStringStartChar = '\''.code.toByte()
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
        val numberChars = setOf(
            dot,
            minus,
            plus,
            exponentIdentifierLowerChar,
            exponentIdentifierUpperChar,
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
        const val falseUpperFirstChar = 'F'.code.toByte()
        const val falseLowerFirstChar = 'f'.code.toByte()
        const val trueUpperFirstChar = 'T'.code.toByte()
        const val trueLowerFirstChar = 't'.code.toByte()
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
        return if (size == 0) {
            NullNode()
        } else {
            parseAnyNode()
        }
    }

    private fun parseAnyNode(): Node {
        skipWhitespacesAndComments()

        if (index < size) {
            return when (bytes[index]) {
                arrayStartChar -> parseArray()
                objectStartChar -> parseObject()
                doubleQuoteStringStartChar -> TextNode(parseDoubleQuotedString())
                singleQuoteStringStartChar -> TextNode(parseSingleQuotedString())
                minus, dot, digit0, digit1, digit2, digit3, digit4, digit5, digit6, digit7, digit8, digit9 ->
                    parseNumber()
                falseUpperFirstChar, falseLowerFirstChar, trueUpperFirstChar, trueLowerFirstChar ->
                    parseBoolean()
                nullUpperFirstChar, nullLowerFirstChar -> parseNull()
                else -> throw ParseException("Invalid character ${getCurrentChar()} at ${getPositionString()}. Expected Array, Object, String, Number, true, false or null.",
                    index)
            }
        } else {
            throw EOFException("Reached end of input but expected any of Array, Object, String, Number, true, false or null.")
        }
    }

    private fun skipWhitespacesAndComments() {
        while (index < size &&
            (bytes[index] == commentStartChar ||
                    bytes[index] == spaceChar ||
                    bytes[index] == newlineChar ||
                    bytes[index] == tabChar ||
                    bytes[index] == backspaceChar ||
                    bytes[index] == feedChar ||
                    bytes[index] == carriageReturnChar)
        ) {
            when (bytes[index]) {
                commentStartChar -> skipComment()
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

    private fun skipComment() {
        if (index < size - 1) {
            index++
            if (bytes[index] == commentStartChar) {
                index++
                while (index < size && bytes[index] != newlineChar) {
                    index++
                }
                index++ // skip the last char, as it is the terminal newline char
            } else if (bytes[index] == multilineCommentSecondStartChar) {
                index++
                while (index < size - 1 && bytes[index] != multilineCommentSecondStartChar && bytes[index + 1] != commentStartChar) {
                    index++
                }
                if (index >= size - 1) {
                    throw EOFException("Unexpected EOF. Expected '*/' at ${getPositionString()} but input is too short.")
                }
                index += 2 // as we did not exceeded the input, it means the char at "index" is "*" and at index+1 it is "/". skip those two chars
            } else {
                throw ParseException("Invalid character ${getCurrentChar()} at ${getPositionString()}. Expected '//' or '/*'.",
                    index)
            }
        } else {
            throw EOFException("Unexpected EOF. Expected '//' or '/*' at ${getPositionString()} but input is too short.")
        }
    }

    private fun parseNumber(): Node.Number {
        /*
          INTEGER: ("-")? ("0" | ["1"-"9"] (["0"-"9"])*)
          DECIMAL: ("-")? ("0" | ["1"-"9"] (["0"-"9"])*) ("." (["0"-"9"])+ | ("." (["0"-"9"])+)? ("e"|"E") ("+"|"-")? (["0"-"9"])+)
         */
        var decimalPointIndex = -1
        var exponentIdentifierIndex = -1
        val hasSign = bytes[index] == minus
        val startIndex = index
        if (hasSign) index++ // skip potential minus at the beginning, so we don't recognize it and falsely throw an error because the exponent before is missing
        while (index < size && bytes[index] in numberChars) {
            if (bytes[index] == dot) {
                if (decimalPointIndex >= 0) {
                    throw ParseException("Invalid character at ${getPositionString()}. Two decimal points found in number.",
                        index)
                } else {
                    decimalPointIndex = index
                }
            } else if (bytes[index] == exponentIdentifierLowerChar || bytes[index] == exponentIdentifierUpperChar) {
                if (decimalPointIndex < 0 || index - decimalPointIndex <= 1) { // if we either have no decimal point (index <0) or the decimal point is right before the exponent, something is wrong
                    throw ParseException("Invalid character at ${getPositionString()}. Found exponent but no preceding decimal point.",
                        index)
                } else if (exponentIdentifierIndex > 0) {
                    throw ParseException("Invalid character at ${getPositionString()}. Two exponents found in number.",
                        index)
                } else {
                    exponentIdentifierIndex = index
                }
            } else if ((bytes[index] == minus || bytes[index] == plus) && exponentIdentifierIndex != index - 1) {
                throw ParseException("Invalid character ${getCurrentChar()} at ${getPositionString()}.", index)
            }
            index++
        }

        val numberAsString = bytes.copyOfRange(startIndex, index).decodeToString()
        return Node.Number.fromString(numberAsString)
    }

    private fun parseBoolean(): BooleanNode {
        val lenFalse = 5 // False
        val lenTrue = 4 // True

        if (index <= size - lenFalse && (bytes[index] == falseUpperFirstChar || bytes[index] == falseLowerFirstChar)) {
            val subStr = bytes.copyOfRange(index, index + lenFalse).decodeToString()
            if (subStr.lowercase() == "false") {
                index += lenFalse
                return BooleanNode(false)
            } else {
                throw ParseException("Invalid character at ${getPositionString()}. Expected false but found ${subStr}.",
                    index)
            }
        } else if (index <= size - lenTrue && (bytes[index] == trueUpperFirstChar || bytes[index] == trueLowerFirstChar)) {
            val subStr = bytes.copyOfRange(index, index + lenTrue).decodeToString()
            if (subStr.lowercase() == "true") {
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

    private fun parseSingleQuotedString(): String {
        return parseString(singleQuoteStringStartChar)
    }

    private fun parseDoubleQuotedString(): String {
        return parseString(doubleQuoteStringStartChar)
    }

    private fun parseString(stringStartEndChar: Byte): String {
        var lastWasEscapeChar = false
        val startIndex = index + 1 // skip first one, as this is the string enclosing character " or '
        while (index < size) {
            index++
            if (bytes[index] == escapeChar) {
                lastWasEscapeChar = true
            } else if (bytes[index] == newlineChar) {
                throw ParseException("Invalid character at ${getPositionString()}. Linebreaks are not allowed in strings.",
                    index)
            } else if (bytes[index] == stringStartEndChar && !lastWasEscapeChar) {
                val endIndex = index // note: indexTo parameter of copyOfRange is exclusive
                index++ // skip closing " or '
                return if (startIndex < endIndex - 1) {
                    bytes.copyOfRange(startIndex, endIndex).decodeToString()
                } else {
                    ""
                }
            } else {
                lastWasEscapeChar = false
            }
        }
        throw EOFException("Unexpected EOF. Expected string closing character ($stringStartEndChar) for string at ${getPositionString()} but input is too short.")
    }

    private fun parseObject(): ObjectNode {
        index++
        val items = mutableMapOf<String, Node>()
        // handle empty object
        if (bytes[index] == objectEndChar) {
            return ObjectNode(items)
        }
        while (index < size) {
            val key = expectObjectKey()
            expectColon()
            val value = parseAnyNode()
            items[key] = value
            if (expectObjectEnd()) return ObjectNode(items)
        }
        throw ParseException("Invalid character at ${getPositionString()}. Expected ',' or '}' but found ${getCurrentChar()}.",
            index)
    }

    private fun expectObjectKey() = if (index < size) {
        skipWhitespacesAndComments()
        if (bytes[index] == doubleQuoteStringStartChar) {
            parseDoubleQuotedString()
        } else if (bytes[index] == singleQuoteStringStartChar) {
            parseSingleQuotedString()
        } else {
            throw ParseException("Invalid character at ${getPositionString()}. Expected string but found ${getCurrentChar()}.",
                index)
        }
    } else {
        throw EOFException("Unexpected EOF. Expected string at ${getPositionString()} but input is too short.")
    }

    private fun expectColon() {
        skipWhitespacesAndComments()
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
            return ArrayNode(items)
        }
        while (index < size) {
            val value = parseAnyNode()
            items.add(value)
            if (expectArrayEnd()) return ArrayNode(items)
        }
        throw ParseException("Invalid character at ${getPositionString()}. Expected ',' or ']' but found ${getCurrentChar()}.",
            index)
    }

    private fun expectArrayEnd(): Boolean = expectEnd(arrayEndChar)

    private fun expectEnd(endChar: Byte): Boolean {
        skipWhitespacesAndComments()
        if (index < size) {
            if (bytes[index] == endChar) {
                index++
                return true
            } else if (bytes[index] == comma) {
                index++
            } else {
                throw ParseException("Invalid character at ${getPositionString()}. Expected ',' or " +
                        "'${endChar.toInt().toChar()}' but found ${getCurrentChar()}.", index)
            }
        }
        return false
    }

    override fun toString(): String = bytes.contentToString()
}