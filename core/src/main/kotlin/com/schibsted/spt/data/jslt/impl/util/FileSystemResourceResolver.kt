package com.schibsted.spt.data.jslt.impl.util

import java.nio.charset.Charset
import kotlin.jvm.JvmOverloads
import java.nio.file.Path
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.ResourceResolver
import java.io.*
import java.nio.charset.StandardCharsets

class FileSystemResourceResolver : ResourceResolver {
    private var rootPath // can be null
            : File?
    private var charset: Charset

    @JvmOverloads
    constructor(rootPath: File? = null, charset: Charset = StandardCharsets.UTF_8) {
        this.rootPath = rootPath
        this.charset = charset
    }

    constructor(rootPath: Path, charset: Charset = StandardCharsets.UTF_8) {
        this.rootPath = rootPath.toAbsolutePath().toFile()
        this.charset = charset
    }

    override fun resolve(jslt: String): Reader {
        return try {
            val file = File(rootPath, jslt)
            val `is`: InputStream = FileInputStream(file)
            InputStreamReader(`is`, charset)
        } catch (e: IOException) {
            throw JsltException("Could not resolve file '$jslt': $e", e)
        }
    }
}