package com.schibsted.spt.data.jslt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.schibsted.spt.data.jslt.impl.util.FileSystemResourceResolver
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class FileSystemResourceResolverTest {
    @Test
    @Throws(IOException::class)
    fun testResolveImportsFromFilesystem() {
        val resolver = FileSystemResourceResolver()
        val e = parse("./src/test/resources/import-from-fs/working1.jslt", resolver)
        Assert.assertEquals(
            readResource("import-from-fs/working1_expected_result.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(e.apply(mapper.readTree("{}")))
        )
    }

    @Test
    @Throws(IOException::class)
    fun testResolveImportsFromFilesystemWithExplicitRootPath() {
        val resolver = FileSystemResourceResolver(File("src/test/resources/import-from-fs"))
        val e = parse("./src/test/resources/import-from-fs/working2.jslt", resolver)
        Assert.assertEquals(
            readResource("import-from-fs/working1_expected_result.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(e.apply(mapper.readTree("{}")))
        )
    }

    @Test
    @Throws(IOException::class)
    fun testResolveImportsFromFilesystemNotWorking() {
        val resolver = FileSystemResourceResolver()
        try {
            parse("./src/test/resources/import-from-fs/wrong_relative_path.jslt", resolver)
            Assert.fail("Expected " + JsltException::class.java.simpleName)
        } catch (e: JsltException) {
            Assert.assertEquals(FileNotFoundException::class.java, e.cause!!.javaClass)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testResolveImportsFromFilesystemWithEncoding() {
        val resolver = FileSystemResourceResolver(
            File("./src/test/resources"), StandardCharsets.ISO_8859_1
        )
        val e = parse("./src/test/resources/character-encoding-master.jslt", resolver)
        val result = e.apply(NullNode.instance)
        Assert.assertEquals("Hei på deg", result.asText())
    }

    @Throws(IOException::class)
    private fun parse(resource: String, resolver: ResourceResolver): Expression {
        return Parser(
            FileReader(File(resource))
        )
            .withResourceResolver(resolver)
            .compile()
    }

    @Throws(IOException::class)
    private fun readResource(path: String): String {
        return try {
            String(
                Files.readAllBytes(
                    Paths.get(
                        javaClass.classLoader
                            .getResource(path).toURI()
                    )
                ), StandardCharsets.UTF_8
            )
        } catch (e: URISyntaxException) {
            throw IOException(e)
        }
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}