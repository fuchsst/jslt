package com.schibsted.spt.data.jslt

import org.junit.Test

class JsltTest : TestBase() {
    @Test
    fun testRewriteObjectRootLevelMatcher() {
        val query = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#2.json",
  "taip" : .type,
  * : .
}"""
        val input = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#1.json",
  "id" : "94b27ca1-8729-4773-986b-1c0517dd6af1",
  "published" : "2017-05-04T09:13:29+02:00",
  "type" : "View",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        val result = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#2.json",
  "id" : "94b27ca1-8729-4773-986b-1c0517dd6af1",
  "published" : "2017-05-04T09:13:29+02:00",
  "taip" : "View",
  "type" : "View",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        check(input, query, result)
    }

    @Test
    fun testRewriteObjectNestedMatcher() {
        val query = """{
  "foo" : {
    "hey" : "på deg",
    * : .
  }
}
"""
        val input = """{
  "foo" : {
    "type" : "View",
    "hey" : "ho"
  }
}
"""
        val result = """{
  "foo" : {
    "hey" : "på deg",
    "type" : "View"
  }
}
"""
        check(input, query, result)
    }

    @Test
    fun testRewriteObjectLetMatcher() {
        val query = """{
  let bar = {* : .}
  "fish" : "barrel",
  "copy" : ${"$"}bar
}"""
        val input = """{
  "if" : "View",
  "else" : "false"
}
"""
        val result = """{
  "fish" : "barrel",
  "copy" : {
    "if" : "View",
    "else" : "false"
  }
}
"""
        check(input, query, result)
    }

    @Test
    fun testProduceEmptyObject() {
        val query = """{
  "fish" : "barrel",
  "copy" : {
    let bar = { * : . }
    "dongle" : ${"$"}bar
  }
}
"""
        val input = """{
  "if" : "View",
  "else" : "false"
}
"""
        val result = """{
  "fish" : "barrel"
}
"""
        check(input, query, result)
    }

    @Test
    fun testProduceCopyOfObject() {
        val query = """{
  "fish" : "barrel",
  "foo" : if (.foo) { * : . }
}
"""
        val input = """{
  "foo" : {
    "type" : "View",
    "hey" : "ho"
  }
}
"""
        val result = """{
  "fish" : "barrel",
  "foo" : {
    "type" : "View",
    "hey" : "ho"
  }
}
"""
        check(input, query, result)
    }

    @Test
    fun testProduceArrayOfTransformedObjects() {
        val query = """{"bar" : [for (.list)
  {"loop" : "for",
   * : . }]
}
"""
        val input = """{
  "list" : [
    {"bar" : 1},
    {"bar" : 2},
    {"bar" : 3},
    {"bar" : 4},
    {"bar" : 5}
  ]
}
"""
        val result = """{
  "bar" : [ {
    "loop" : "for",
    "bar" : 1
  }, {
    "loop" : "for",
    "bar" : 2
  }, {
    "loop" : "for",
    "bar" : 3
  }, {
    "loop" : "for",
    "bar" : 4
  }, {
    "loop" : "for",
    "bar" : 5
  } ]
}
"""
        check(input, query, result)
    }

    @Test
    fun testRemoveType() {
        val query = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#2.json",
  "taip" : .type ,
  * - "type" : .
}"""
        val input = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#1.json",
  "id" : "94b27ca1-8729-4773-986b-1c0517dd6af1",
  "published" : "2017-05-04T09:13:29+02:00",
  "type" : "View",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        val result = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#2.json",
  "id" : "94b27ca1-8729-4773-986b-1c0517dd6af1",
  "published" : "2017-05-04T09:13:29+02:00",
  "taip" : "View",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        check(input, query, result)
    }

    @Test
    fun testRemoveTypeAndId() {
        val query = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#2.json",
  "taip" : .type,
  * - "type", "id" : .
}"""
        val input = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#1.json",
  "id" : "94b27ca1-8729-4773-986b-1c0517dd6af1",
  "published" : "2017-05-04T09:13:29+02:00",
  "type" : "View",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        val result = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#2.json",
  "published" : "2017-05-04T09:13:29+02:00",
  "taip" : "View",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        check(input, query, result)
    }

    @Test
    fun testRemoveTypeRemove() {
        val query = """{
  "type" : if (.type and .type != "View") .type,
  * : .
}"""
        val input = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#1.json",
  "id" : "94b27ca1-8729-4773-986b-1c0517dd6af1",
  "published" : "2017-05-04T09:13:29+02:00",
  "type" : "View",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        val result = """{
  "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#1.json",
  "id" : "94b27ca1-8729-4773-986b-1c0517dd6af1",
  "published" : "2017-05-04T09:13:29+02:00",
  "environmentId" : "urn:schibsted.com:environment:uuid",
  "url" : "http://www.aftenposten.no/"
}
"""
        check(input, query, result)
    }

    @Test
    fun testRemoveBazNested() {
        val query = """{
    "foo" : {
    "bar" : {
      * - "baz" : .
    }
    }
}
"""
        val input = """{
    "foo" : {
    "bar" : {
        "baz" : 1,
        "quux" : 2
    }
    }
}
"""
        val result = """{
    "foo" : {
    "bar" : {
        "quux" : 2
    }
    }
}
"""
        check(input, query, result)
    }
}