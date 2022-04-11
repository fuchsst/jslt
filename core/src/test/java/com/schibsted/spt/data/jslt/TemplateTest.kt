package com.schibsted.spt.data.jslt

import org.junit.Test

/**
 * Test cases verifying templates.
 */
class TemplateTest : TestBase() {
    @Test
    fun testTemplate() {
        check("{\"foo\" : 2}", "{\"bar\" : .foo}", "{\"bar\" : 2}")
    }

    @Test
    fun testTemplateNull() {
        check("{\"foo\" : 2}", "{\"bar\" : .foo, \"baz\" : .bar}", "{\"bar\" : 2}")
    }

    @Test
    fun testIfTemplate() {
        val template = "{\"bar\" : if (.foo) .foo else .bar}"
        check("{\"foo\" : 2}", template, "{\"bar\" : 2}")
        check("{\"bar\" : 2}", template, "{\"bar\" : 2}")
        check("{\"baz\" : 2}", template, "{}")
    }

    @Test
    fun testComment() {
        check(
            "{\"foo\" : 2}", """// tuut tuut
{"bar" : .foo}""", "{\"bar\" : 2}"
        )
    }

    @Test
    fun testTopLevelLet() {
        check(
            "{\"foo\" : 2}", "let foo = 2 " +
                    "{\"bar\" : \$foo}", "{\"bar\" : 2}"
        )
    }

    @Test
    fun testObjectLet() {
        check(
            "{\"foo\" : 2}", "{let foo = 2 " +
                    "\"bar\" : \$foo}", "{\"bar\" : 2}"
        )
    }

    @Test
    fun testIfLet() {
        check(
            "{\"foo\" : 2}",
            "{\"bar\" : if (.foo) " +
                    "             let var = .foo " +
                    "             \$var " +
                    "           else .bar }",
            "{\"bar\" : 2}"
        )
    }

    @Test
    fun testIfElseLet() {
        check(
            "{\"bar\" : 2}",
            "{\"bar\" : if (.foo) " +
                    "             .foo " +
                    "           else " +
                    "             let var = 234 " +
                    "             \$var }",
            "{\"bar\" : 234}"
        )
    }

    @Test
    fun testBasicMatching() {
        check(
            "{\"bar\" : 2, \"baz\" : 14}",
            "{* : .}",
            "{\"bar\" : 2, " +
                    " \"baz\" : 14 }"
        )
    }

    @Test
    fun testBasicMatching2() {
        check(
            "{\"bar\" : 2, \"baz\" : 14}",
            "{\"bille\" : 100, " +
                    " \"foo\" : .bar, " +
                    " \"bar\" : 200, " +
                    " * : .}",
            "{\"bille\" : 100, " +
                    " \"foo\" : 2, " +
                    " \"bar\" : 200, " +
                    " \"baz\" : 14 }"
        )
    }

    @Test
    fun testMatchArray() {
        check(
            "[1,2,3,4,5]",
            "{\"bille\" : 100, " +
                    " \"foo\" : .bar, " +
                    " \"bar\" : 200, " +
                    " * : .}",
            "{\"bille\" : 100, " +
                    " \"bar\" : 200}"
        )
    }

    @Test
    fun testMatchArrayOnly() {
        check(
            "[1,2,3,4,5]",
            "{\"bille\" : 100, " +
                    " \"bar\" : 200, " +
                    " * : .}",
            "{\"bille\" : 100, " +
                    " \"bar\" : 200}"
        )
    }

    @Test
    fun testMatchingNested() {
        check(
            "{\"bille\" : { " +
                    "   \"type\" : 14, " +
                    "   \"hey\" : 18 " +
                    " } " +
                    "}",
            "{\"bille\" : { " +
                    "   \"hey\" : 22, " +
                    "   * : . " +
                    " } " +
                    "}",
            "{\"bille\" : { " +
                    "   \"type\" : 14, " +
                    "   \"hey\" : 22 " +
                    " } " +
                    "}"
        )
    }

    @Test
    fun testMatchingInLet() {
        check(
            "{ " +
                    "  \"if\" : \"View\", " +
                    "  \"else\" : false " +
                    "}",
            "{ " +
                    "  let bar = {* : .} " +
                    "  \"fish\" : \"barrel\", " +
                    "  \"copy\" : \$bar " +
                    "}",
            "{" +
                    "  \"fish\" : \"barrel\", " +
                    "  \"copy\" : { " +
                    "    \"if\" : \"View\", " +
                    "    \"else\" : false " +
                    "  } " +
                    "}"
        )
    }

    @Test
    fun testMatchingInNestedLet() {
        check(
            "{ " +
                    "  \"if\" : \"View\", " +
                    "  \"else\" : false " +
                    "}",
            "{ " +
                    "  \"fish\" : \"barrel\", " +
                    "  \"copy\" : { " +
                    "    let bar = {* : .} " +
                    "    \"dongle\" : \$bar " +
                    "  } " +
                    "}",
            "{" +
                    "  \"fish\" : \"barrel\" " +
                    "}"
        )
    }

    @Test
    fun testMatchingInIf() {
        check(
            "{\"bille\" : { " +
                    "   \"type\" : 14, " +
                    "   \"hey\" : 18 " +
                    " } " +
                    "}",
            "{ " +
                    "  \"fish\" : \"barrel\", " +
                    "  \"bille\" : if ( .bille ) {* : .} " +
                    "}",
            "{" +
                    "  \"fish\" : \"barrel\", " +
                    "  \"bille\" : { " +
                    "    \"type\" : 14, " +
                    "    \"hey\" : 18 " +
                    "  } " +
                    "}"
        )
    }

    @Test
    fun testMatchingRemove() {
        check(
            "{ " +
                    "  \"schema\" : \"http://schemas.schibsted.io/thing/pulse-simple.json#1.json\", " +
                    "  \"id\" : \"94b27ca1-8729-4773-986b-1c0517dd6af1\", " +
                    "  \"published\" : \"2017-05-04T09:13:29+02:00\", " +
                    "  \"type\" : \"View\", " +
                    "  \"environmentId\" : \"urn:schibsted.com:environment:uuid\", " +
                    "  \"url\" : \"http://www.aftenposten.no/\" " +
                    "}",
            "{ " +
                    "  \"schema\" : \"http://schemas.schibsted.io/thing/pulse-simple.json#2.json\", " +
                    "  \"taip\" : \"View\", " +
                    "  * - type : . " +
                    "}",
            "{ " +
                    "  \"schema\" : \"http://schemas.schibsted.io/thing/pulse-simple.json#2.json\", " +
                    "  \"taip\" : \"View\", " +
                    "  \"id\" : \"94b27ca1-8729-4773-986b-1c0517dd6af1\", " +
                    "  \"published\" : \"2017-05-04T09:13:29+02:00\", " +
                    "  \"environmentId\" : \"urn:schibsted.com:environment:uuid\", " +
                    "  \"url\" : \"http://www.aftenposten.no/\" " +
                    "}"
        )
    }

    @Test
    fun testMatchingInFor() {
        check(
            "{ " +
                    "   \"list\" : [ " +
                    "     {\"bar\": 1}, " +
                    "     {\"bar\": 2} " +
                    "   ] " +
                    "}",
            "{ " +
                    "   \"foo\" : [for ( .list ) " +
                    "     {\"loop\" : \"for\", " +
                    "     * : . }] " +
                    "}",
            "{ " +
                    "   \"foo\" : [{ " +
                    "     \"loop\" : \"for\", " +
                    "     \"bar\" : 1 " +
                    "   }, { " +
                    "     \"loop\" : \"for\", " +
                    "     \"bar\" : 2 " +
                    "   } " +
                    "]}"
        )
    }

    @Test
    fun testMatchingNoSuchObject() {
        check(
            "null",
            "{ " +
                    "  \"foo\" : 5, " +
                    "  * : . " +
                    "}",
            "{ " +
                    "   \"foo\" : 5 " +
                    "}"
        )
    }

    @Test
    fun testMatchingNotAnAobject() {
        check(
            "{ " +
                    "   \"foo\" : 5, " +
                    "   \"bar\" : 2 " +
                    "}",
            "{ " +
                    "  \"foo\" : { " +
                    "    \"bar\" : .bar, " +
                    "    * : . " +
                    "  } " +
                    "}",
            "{\"foo\" : {\"bar\" : 2}}"
        )
    }

    @Test
    fun testMatchingInForInsideAnArray() {
        check(
            "[{ " +
                    "   \"foo\" : 5, " +
                    "   \"bar\" : 2 " +
                    "}]",  // convoluted template to say: take the top-level array,
            // transform the objects it contains into the same objects,
            // and wrap the whole thing in an array
            "[[for (.) {* : .}]]",
            "[[{\"foo\" : 5, \"bar\" : 2}]]"
        )
    }

    @Test
    fun testMatchingInAnArray() {
        error(
            "[{* : .}]",
            "array"
        )
    }

    @Test
    fun testHandleTrickyTransform() {
        check(
            "{}",
            "{" +
                    "  \"provider\": {" +
                    "    let urn = .provider.\"@id\"" +
                    "  }," +
                    "  * : ." +
                    "}",
            "{}"
        )
    }

    @Test
    fun testDuplicateKeyIsInvalid() {
        error(
            "{" +
                    " \"foo\" : 1, " +
                    " \"foo\" : 2 " +
                    "}",
            "duplicate"
        )
    }

    fun testHandleTrickyTransformV2() {
        check(
            "{\"provider\" : {\"@id\" : 22}}",
            "{" +
                    "  \"provider\": {" +
                    "    let urn = .provider.\"@id\"" +
                    "    \"urn\" : \$urn " +
                    "  }," +
                    "  * : ." +
                    "}",
            "{\"provider\" : {\"urn\" : 22}}"
        )
    }

    @Test
    fun testObjectLetUsingVariable() {
        check(
            "{\"foo\" : 22}",
            "{" +
                    "  let v = .foo " +
                    "  let vv = \$v + 10 " +
                    "  \"bar\" : \$vv + 10, " +
                    "  * : ." +
                    "}",
            "{\"foo\" : 22, \"bar\" : 42}"
        )
    }

    @Test
    fun testObjectLetAfterFunction() {
        check(
            "{\"foo\" : 22}",
            "def fun(v) " +
                    "  \$v / 2 " +
                    "{" +
                    "  let v = .foo " +
                    "  let vv = \$v + 10 " +
                    "  \"bar\" : fun(\$vv + 10), " +
                    "  * : ." +
                    "}",
            "{\"foo\" : 22, \"bar\" : 21}"
        )
    }

    @Test
    fun testDynamicKeys() {
        check(
            load("user-external.json"),
            load("user-external2cdp.jslt"),
            load("cdp.json")
        )
    }
}