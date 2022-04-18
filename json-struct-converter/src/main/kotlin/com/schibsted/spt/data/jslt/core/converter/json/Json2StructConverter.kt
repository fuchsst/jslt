package com.schibsted.spt.data.jslt.core.converter.json

import com.schibsted.spt.data.jslt.core.struct.*
import java.io.EOFException
import java.io.InputStream
import java.text.ParseException

class Json2StructConverter(private val bytes: ByteArray) : StructConverter {
    companion object {
        // we can safely do byte comparison on those characters, as UTF-8 always marks every non-ascii char
        // with the highest bit set in every sub-byte of the encoded character
        private const val arrayStartChar = '['.code.toByte()
        private const val arrayEndChar = ']'.code.toByte()
        private const val objectStartChar = '{'.code.toByte()
        private const val objectEndChar = '}'.code.toByte()
        private const val stringQuoteChar = '"'.code.toByte()
        private const val escapeChar = '\\'.code.toByte()
        private val allowedEscapedChars = setOf('\"'.code.toByte(),
            '\\'.code.toByte(),
            '/'.code.toByte(),
            'b'.code.toByte(),
            'f'.code.toByte(),
            'n'.code.toByte(),
            'r'.code.toByte(),
            't'.code.toByte()
        )
        private const val unicodeSequencePrefixChar = 'u'.code.toByte()
        private const val backspaceChar = '\b'.code.toByte()
        private const val carriageReturnChar = '\r'.code.toByte()

        // const val feedChar = '\u000C'.code.toByte()
        private const val newlineChar = '\n'.code.toByte()
        private const val spaceChar = ' '.code.toByte()
        private const val tabChar = '\t'.code.toByte()
        private val whitespaceChars = setOf(backspaceChar, carriageReturnChar, newlineChar, spaceChar, tabChar)
        private const val comma = ','.code.toByte()
        private const val colon = ':'.code.toByte()
        private const val minus = '-'.code.toByte()
        private const val plus = '+'.code.toByte()
        private const val dot = '.'.code.toByte()
        private const val digit0 = '0'.code.toByte()
        private const val digit1 = '1'.code.toByte()
        private const val digit2 = '2'.code.toByte()
        private const val digit3 = '3'.code.toByte()
        private const val digit4 = '4'.code.toByte()
        private const val digit5 = '5'.code.toByte()
        private const val digit6 = '6'.code.toByte()
        private const val digit7 = '7'.code.toByte()
        private const val digit8 = '8'.code.toByte()
        private const val digit9 = '9'.code.toByte()
        private const val exponentIdentifierLowerChar = 'e'.code.toByte()
        private const val exponentIdentifierUpperChar = 'E'.code.toByte()
        private val digits = setOf(
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
        private val hexDigits = digits + setOf(
            'a'.code.toByte(),
            'b'.code.toByte(),
            'c'.code.toByte(),
            'd'.code.toByte(),
            'e'.code.toByte(),
            'f'.code.toByte(),
            'A'.code.toByte(),
            'B'.code.toByte(),
            'C'.code.toByte(),
            'D'.code.toByte(),
            'E'.code.toByte(),
            'F'.code.toByte())
        private const val falseFirstChar = 'f'.code.toByte()
        private const val trueFirstChar = 't'.code.toByte()
        private const val nullUpperFirstChar = 'N'.code.toByte()
        private const val nullLowerFirstChar = 'n'.code.toByte()
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
        skipBOM()
        val result = parseAnyNode()
        skipWhitespaces()
        if (index < size) {
            throwParseError("Input contains trailing characters that are not part of the Json parsed Json.")
        }
        return result
    }

    private fun throwParseError(errMsg: String): Nothing {
        throw ParseException("Invalid character ${getCurrentChar()} at ${getPositionString()}: $errMsg", index)
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
                else -> throwParseError("Expected Array, Object, String, Number, true, false or null.")
            }
        } else {
            throw EOFException("Reached end of input but expected any of Array, Object, String, Number, true, false or null.")
        }
    }

    private fun skipBOM() {
        if (index <= 2 && size >= 2) {
            if (bytes[0] == (0xEF).toByte() && bytes[1] == (0xBB).toByte() && bytes[2] == (0xBF).toByte()) {
                index += 3
            }
        }
    }

    private fun skipWhitespaces() {
        while (index < size && bytes[index] in whitespaceChars) {
            if (bytes[index] == newlineChar) {
                line++
                index++
                column = 1
            } else {
                index++
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
            throwParseError("Integer values should not start with 0.")
        }
        if (index == size ||
            (bytes[index] != dot &&
                    bytes[index] != exponentIdentifierLowerChar && bytes[index] != exponentIdentifierUpperChar)
        ) {
            return Node.Number.Integral.fromString("$sign$intAsString")
        } else {
            val decimalPart = if (index < size && bytes[index] == dot) {
                if (intAsString.isEmpty()) {
                    throwParseError("Real number without integer part.")
                }
                index++
                if (index < size && bytes[index] !in digits) {
                    throwParseError("Expected digit after decimal point.")
                }
                val decimalsStartIndex = index
                while (index < size && bytes[index] in digits) {
                    index++
                }
                val decimalsAsString = bytes.copyOfRange(decimalsStartIndex, index).decodeToString()
                if (decimalsAsString.isEmpty()) {
                    throwParseError("Number with decimal dot but without decimal part.")
                }
                ".$decimalsAsString"
            } else {
                ""
            }

            if (index == size || (bytes[index] != exponentIdentifierLowerChar && bytes[index] != exponentIdentifierUpperChar)) {
                return Node.Number.Decimal.fromString("$sign$intAsString$decimalPart")
            } else {
                if (index < size && (bytes[index] == exponentIdentifierLowerChar || bytes[index] == exponentIdentifierUpperChar)) {
                    index++
                    if (index < size && (bytes[index] !in digits && bytes[index] != minus && bytes[index] != plus)) {
                        throwParseError("Expected digit or +/- after exponent.")
                    }
                    val exponentStartIndex = index
                    if (index < size && (bytes[index] == minus || bytes[index] == plus)) {
                        index++
                    }
                    while (index < size && bytes[index] in digits) {
                        index++
                    }
                    val exponentAsString = bytes.copyOfRange(exponentStartIndex, index).decodeToString()
                    return Node.Number.Decimal.fromString("$sign$intAsString$decimalPart", exponentAsString)
                }
            }

        }
        throwParseError("Unexpected parser state.")
    }

    private fun parseBoolean(): BooleanNode {
        val falseString = "false"
        val trueString = "true"
        val lenFalse = falseString.length
        val lenTrue = trueString.length

        if (index <= size - lenFalse && bytes[index] == falseFirstChar) {
            val subStr = bytes.copyOfRange(index, index + lenFalse).decodeToString()
            if (subStr == falseString) {
                index += lenFalse
                return BooleanNode(false)
            } else {
                throwParseError("Expected false but found ${subStr}.")
            }
        } else if (index <= size - lenTrue && bytes[index] == trueFirstChar) {
            val subStr = bytes.copyOfRange(index, index + lenTrue).decodeToString()
            if (subStr == trueString) {
                index += lenTrue
                return BooleanNode(true)
            } else {
                throwParseError("Expected true but found ${subStr}.")
            }
        } else {
            throw EOFException("Unexpected EOF. Expected 'true' or 'false' at ${getPositionString()} but input is too short.")
        }
    }

    private fun parseNull(): NullNode {
        val nullString = "null"
        val lenNull = nullString.length

        if (index <= size - lenNull && (bytes[index] == nullUpperFirstChar || bytes[index] == nullLowerFirstChar)) {
            val subStr = bytes.copyOfRange(index, index + lenNull).decodeToString()

            if (subStr.lowercase() == nullString) {
                index += lenNull
                return NullNode()
            } else {
                throwParseError("Expected null but found ${subStr}.")
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
            if (bytes[index] == escapeChar && !lastWasEscapeChar) { // escape char is only an escape char if it was not escaped itself
                lastWasEscapeChar = true
            } else if (bytes[index] == newlineChar) {
                throwParseError("Linebreaks are not allowed in strings.")
            } else if (bytes[index] == stringQuoteChar && !lastWasEscapeChar) { // we reached the end of the string, so we return what we collected
                val endIndex = index // note: indexTo parameter of copyOfRange is exclusive
                index++ // skip closing " or '
                return if (startIndex < endIndex) {
                    bytes.copyOfRange(startIndex, endIndex).decodeToString()
                } else {
                    ""
                }
            } else {
                if (lastWasEscapeChar) {
                    validateEscapedChar()
                } else {
                    expectNonControlChar()
                }
                lastWasEscapeChar = false
            }
        }
        throw EOFException("Unexpected EOF. Expected string closing character (\") for string at ${getPositionString()} but input is too short.")
    }

    private fun expectNonControlChar() {
        if (bytes[index].toUByte() < 32u) {
            throwParseError("Unescaped control character.")
        }
    }

    private fun validateEscapedChar() {
        if (bytes[index] == unicodeSequencePrefixChar) {
            index++
            if (index < size - 4 && bytes[index] in hexDigits && bytes[index + 1] in hexDigits && bytes[index + 2] in hexDigits && bytes[index + 3] in hexDigits) {
                index += 3
            } else {
                throwParseError("Found unicode escape but unicode-hex code was invalid.")
            }
        } else if (bytes[index] !in allowedEscapedChars) {
            throwParseError("Invalid escaped character.")
        }
    }

    private fun parseObject(): ObjectNode {
        index++
        val items = mutableMapOf<String, Node>()
        // handle empty object
        skipWhitespaces()
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
        throwParseError("Expected ',' or '}'.")
    }

    private fun expectObjectKey() = if (index < size) {
        skipWhitespaces()
        if (bytes[index] == stringQuoteChar) {
            parseString()
        } else {
            throwParseError("Expected string.")
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
                throwParseError("Expected ':'.")
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
        skipWhitespaces()
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
        throwParseError("Expected ',' or ']'.")
    }

    private fun expectArrayEnd(): Boolean = expectEnd(arrayEndChar)

    private fun expectComma() {
        skipWhitespaces()
        if (index < size) {
            if (bytes[index] == comma) {
                index++
            } else {
                throwParseError("Expected ','.")
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