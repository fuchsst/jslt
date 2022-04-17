import com.schibsted.spt.data.jslt.core.converter.json.Json2StructConverter
import com.schibsted.spt.data.jslt.core.struct.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.EOFException
import java.math.BigInteger

class Json2StructConverterTest {

    @Test
    fun testParseEmpty() {
        val given = "  \n"
        assertThrows<EOFException>("Expected to fail on empty input, but no exception was thrown!") {
            Json2StructConverter(given).asStruct()
        }
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
        val given = "false"
        val expected = BooleanNode(false)
        val actual = Json2StructConverter(given).asStruct()

        assertEquals(expected, actual)
    }

    @Test
    fun testParseTrueOnly() {
        val given = "true"
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
    fun testParseObjectWithEveryDatatype() {
        val given = "{" +
                " \"key1\": true,\n" +
                " \"key2\": false,\n" +
                " \"key3\": null,\n" +
                " \"key4\": 12345,\n" +
                " \"key5\": -1.1,\n" +
                " \"key6\": \"some string\",\n" +
                " \"key7\": { \"sub key\" : \"sub value\"},\n" +
                " \"key8\": [null, 1,2,3]\n" +
                "}"
        val expected = ObjectNode(mapOf(
            "key1" to BooleanNode(true),
            "key2" to BooleanNode(false),
            "key3" to NullNode(),
            "key4" to IntNode(12345),
            "key5" to DecimalNode(-1.1),
            "key6" to TextNode("some string"),
            "key7" to ObjectNode(mapOf("sub key" to TextNode("sub value"))),
            "key8" to ArrayNode(listOf(NullNode(), IntNode(1), IntNode(2), IntNode(3))),
        ))
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