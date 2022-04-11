package com.schibsted.spt.data.jslt

import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.*

class BenchmarkHashTest : TestBase() {
    @Test
    @Ignore
    fun benchmarkSha256Hex() {
        val start = Instant.now()
        val iterations = 2000000
        for (i in 0 until iterations) {
            execute(input, "sha256-hex(.)")
        }
        val end = Instant.now()
        val elapsed = Duration.between(start, end)
        println("sha256-hex " + iterations + " iterations took " + elapsed.seconds + " seconds. " + iterations.toFloat() / elapsed.seconds + " iterations/second")
    }

    @Test
    @Ignore
    fun benchmarkHashint() {
        val start = Instant.now()
        val iterations = 2000000
        for (i in 0 until iterations) {
            execute(input, "hash-int(.)")
        }
        val end = Instant.now()
        val elapsed = Duration.between(start, end)
        println("hash-int " + iterations + " iterations took " + elapsed.seconds + " seconds. " + iterations.toFloat() / elapsed.seconds + " iterations/second")
    }

    companion object {
        private var input: String? = null

        @BeforeClass
        fun setUp() {
            val `is` = Thread.currentThread().contextClassLoader.getResourceAsStream("json-document-1.json")
            input = Scanner(`is`, "utf-8").useDelimiter("\\Z").next() //convert InputStream to String
        }
    }
}