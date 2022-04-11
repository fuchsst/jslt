package com.schibsted.spt.data.jslt

import org.junit.Test

/**
 * Test cases for imports.
 */
class ImportTest : TestBase() {
    @Test
    fun testIdFunction() {
        check(
            "{}", "import \"module.jstl\" as m " +
                    "m:id(5) ", "5"
        )
    }

    @Test
    fun testModuleFunction() {
        check(
            "{}", "import \"module-body.jstl\" as m " +
                    "m(5) ", "27"
        )
    }

    @Test
    fun testModuleNoBodyFunction() {
        error(
            "import \"module.jstl\" as m " +
                    "m(5) ", "body"
        )
    }

    @Test
    fun testImportGraph() {
        // we import A -> B, A -> C -> B, and this should be fine
        check(
            "{}", "import \"a-1.jstl\" as a " +
                    "a(.)", "1"
        )
    }

    @Test
    fun testCyclicImport() {
        error(
            "import \"a-2.jstl\" as m " +
                    "m(5) ", "already imported"
        )
    }

    @Test
    fun testImportUsesExtensionFunction() {
        check(
            "{}",
            "import \"uses-test.jstl\" as u " +
                    "u(.)",
            "42", emptyMap(), setOf(TestFunction())
        )
    }

    // --- test the count function
    @Test
    fun testCountKeys() {
        check(
            "{}",
            "import \"functions.jstl\" as f " +
                    "f:count(.) ", "0"
        )
    }

    @Test
    fun testCountKeysNumber() {
        check(
            "0",
            "import \"functions.jstl\" as f " +
                    "f:count(.) ", "0"
        )
    }

    @Test
    fun testCountKeysOne() {
        check(
            "{\"foo\" : 0}",
            "import \"functions.jstl\" as f " +
                    "f:count(.) ", "1"
        )
    }

    @Test
    fun testCountKeysTwo() {
        check(
            "{\"foo\" : 0, \"bar\" : 1}",
            "import \"functions.jstl\" as f " +
                    "f:count(.) ", "2"
        )
    }

    @Test
    fun testCountKeysTwoRecursive() {
        check(
            "{\"foo\" : 0, \"bar\" : {\"baz\" : []}}",
            "import \"functions.jstl\" as f " +
                    "f:count(.) ", "3"
        )
    }

    @Test
    fun testCountKeysTwoRecursiveArray() {
        check(
            "{\"foo\" : 0, \"bar\" : {\"baz\" : [{\"foo\" : 0, \"bar\" : 1}]}}",
            "import \"functions.jstl\" as f " +
                    "f:count(.) ", "5"
        )
    } // FIXME: verify that function passed in to top-level parser is also
    // available when parsing imported modules
}