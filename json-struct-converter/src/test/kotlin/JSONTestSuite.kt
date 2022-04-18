import com.schibsted.spt.data.jslt.core.converter.json.Json2StructConverter
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.InputStream
import java.nio.file.Paths

/**
 * Tests if parser fails/passes as expected against the JSONTestSuite files from
 * https://github.com/nst/JSONTestSuite
 */
class JSONTestSuite {
    @ParameterizedTest
    @MethodSource("validJsonList")
    @Suppress("UNUSED_PARAMETER")
    fun `Parses valid Json`(jsonName: String, json: InputStream) {
        json.use { Json2StructConverter(it).asStruct() }
    }

    @ParameterizedTest
    @MethodSource("invalidJsonList")
    fun `Throws exception for invalid Json`(jsonName: String, json: InputStream) {
        assertThrows<Throwable>("$jsonName: expected to fail, but no exception was thrown!") {
            json.use { Json2StructConverter(it).asStruct() }.toString()
        }
    }

    companion object {
        @JvmStatic
        fun validJsonList() = Paths.get("src", "test", "resources", "valid_json")
            .toFile()
            .walk()
            .filter { it.isFile && it.extension == "json" }
            .sorted()
            .map { Arguments.of(it.nameWithoutExtension, it.inputStream()) }
            .toList()

        @JvmStatic
        fun invalidJsonList() = Paths.get("src", "test", "resources", "invalid_json")
            .toFile()
            .walk()
            .filter { it.isFile && it.extension == "json" }
            .sorted()
            .map { Arguments.of(it.nameWithoutExtension, it.inputStream()) }
            .toList()
    }
}



