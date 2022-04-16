import com.schibsted.spt.data.jslt.core.converter.json.Json2StructConverter
import com.schibsted.spt.data.jslt.core.struct.*

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class Json2StructConverterTest {

    @Test
    fun testParseEmpty() {
        val given = ""
        val expected = NullNode()
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseStringOnly() {
        val given = "\"some string\""
        val expected = TextNode("some string")
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseFalseOnly() {
        val given = "FalSe"
        val expected = BooleanNode(false)
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseTrueOnly() {
        val given = "True"
        val expected = BooleanNode(true)
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseNullOnly() {
        val given = "nuLL"
        val expected = NullNode()
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseIntNumberOnly() {
        val given = "2147483647"
        val expected = IntNode(Int.MAX_VALUE)
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseNegativeIntNumberOnly() {
        val given = "-2147483648"
        val expected = IntNode(Int.MIN_VALUE)
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseLongNumberOnly() {
        val given = "9223372036854775807"
        val expected = LongNode(Long.MAX_VALUE)
        val actual = Json2StructConverter(given).asStruct()

        println(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun testParseNegativeLongNumberOnly() {
        val given = "-9223372036854775808"
        val expected = LongNode(Long.MIN_VALUE)
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseBigIntNumberOnly() {
        val given = "999999999999999999999999999999999999"
        val expected = BigIntNode(BigInteger("999999999999999999999999999999999999"))
        val actual = Json2StructConverter(given).asStruct()

        println(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun testParseNegativeBigIntNumberOnly() {
        val given = "-999999999999999999999999999999999999"
        val expected = BigIntNode(BigInteger("-999999999999999999999999999999999999"))
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseEmptyObject() {
        val given = "{}"
        val expected = ObjectNode(emptyMap())
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseEmptyArray() {
        val given = "[]"
        val expected = ArrayNode(emptyList())
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseBooleanArray() {
        val given = "[true, false, null]"
        val expected = ArrayNode(listOf(BooleanNode(true), BooleanNode(false), NullNode()))
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }
}