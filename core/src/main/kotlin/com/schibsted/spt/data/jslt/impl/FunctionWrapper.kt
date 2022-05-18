// Copyright 2018 Schibsted Marketplaces Products & Technology As
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.schibsted.spt.data.jslt.impl

import com.fasterxml.jackson.databind.node.*
import com.schibsted.spt.data.jslt.Function
import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.core.struct.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class FunctionWrapper(override val name: String, private val method: Method) : Function {
    private val converters: Array<ToJavaConverter?>
    private val returnConverter: ToJsonConverter = makeJsonConverter(method.returnType)

    override val minArguments: Int = method.parameterCount
    override val maxArguments: Int = method.parameterCount

    override fun call(input: Node, arguments: Array<Node>): Node {
        val args = arrayOfNulls<Any>(arguments.size)
        for (ix in arguments.indices) args[ix] = converters[ix]!!.convert(arguments[ix])
        return try {
            val result = method.invoke(null, *args)
            returnConverter.convert(result)
        } catch (e: IllegalAccessException) {
            throw JsltException("Couldn't call $method", e)
        } catch (e: InvocationTargetException) {
            throw JsltException("Couldn't call $method", e)
        }
    }

    // ===== TO JAVA
    internal interface ToJavaConverter {
        fun convert(node: Node): Any?
    }

    companion object {
        private val toJava: MutableMap<Class<*>?, ToJavaConverter?> = HashMap()
        private fun makeJavaConverter(type: Class<*>): ToJavaConverter {
            return toJava[type] ?: throw JsltException("Cannot build converter to $type")
        }

        private val toJson: MutableMap<Class<*>?, ToJsonConverter?> = HashMap()
        private fun makeJsonConverter(type: Class<*>): ToJsonConverter {
            return toJson[type]
                ?: throw JsltException("Cannot build converter from $type")
        }

        init {
            toJava[String::class.java] = StringJavaConverter()
            toJava[Int::class.javaPrimitiveType] = IntJavaConverter()
            toJava[Long::class.javaPrimitiveType] = LongJavaConverter()
            toJava[Boolean::class.javaPrimitiveType] = BooleanJavaConverter()
            toJava[Double::class.javaPrimitiveType] = DoubleJavaConverter()
            toJava[Float::class.javaPrimitiveType] = DoubleJavaConverter()
        }

        init {
            toJson[String::class.java] = StringJsonConverter()
            toJson[Long::class.javaPrimitiveType] = LongJsonConverter()
            toJson[Int::class.javaPrimitiveType] = IntJsonConverter()
            toJson[Boolean::class.javaPrimitiveType] = BooleanJsonConverter()
            toJson[Double::class.javaPrimitiveType] = DoubleJsonConverter()
            toJson[Float::class.javaPrimitiveType] = FloatJsonConverter()
        }
    }

    internal class StringJavaConverter : ToJavaConverter {
        override fun convert(node: Node): Any? {
            return if (node.isNull) null else if (node.isTextual) node.asText() else throw JsltException("Could not convert $node to string")
        }
    }

    internal class LongJavaConverter : ToJavaConverter {
        override fun convert(node: Node): Any {
            return if (!node.isNumber) throw JsltException("Cannot convert $node to long") else node.asLong()
        }
    }

    internal class IntJavaConverter : ToJavaConverter {
        override fun convert(node: Node): Any {
            return if (!node.isNumber) throw JsltException("Cannot convert $node to int") else node.asInt()
        }
    }

    internal class BooleanJavaConverter : ToJavaConverter {
        override fun convert(node: Node): Any {
            return if (!node.isBoolean) throw JsltException("Cannot convert $node to boolean") else node.asBoolean()
        }
    }

    internal class DoubleJavaConverter : ToJavaConverter {
        override fun convert(node: Node): Any {
            return if (!node.isNumber) throw JsltException("Cannot convert $node to double") else node.asDouble()
        }
    }

    // ===== TO JSON
    internal interface ToJsonConverter {
        fun convert(node: Any?): Node
    }

    internal class StringJsonConverter : ToJsonConverter {
        override fun convert(node: Any?): Node {
            return if (node == null) NullNode.instance else TextNode(node as String)
        }
    }

    internal class LongJsonConverter : ToJsonConverter {
        override fun convert(node: Any?): Node {
            return if (node == null) NullNode.instance else LongNode(node as Long)
        }
    }

    internal class IntJsonConverter : ToJsonConverter {
        override fun convert(node: Any?): Node {
            return if (node == null) NullNode.instance else IntNode(node as Int)
        }
    }

    internal class BooleanJsonConverter : ToJsonConverter {
        override fun convert(node: Any?): Node {
            return if (node == null) NullNode.instance else if (node as Boolean) BooleanNode.TRUE else BooleanNode.FALSE
        }
    }

    internal class DoubleJsonConverter : ToJsonConverter {
        override fun convert(node: Any?): Node {
            return if (node == null) NullNode.instance else DoubleNode((node as Double?)!!)
        }
    }

    internal class FloatJsonConverter : ToJsonConverter {
        override fun convert(node: Any?): Node {
            return if (node == null) NullNode.instance else FloatNode((node as Float?)!!)
        }
    }

    init {
        val paramTypes = method.parameterTypes
        converters = arrayOfNulls(paramTypes.size)
        for (ix in paramTypes.indices) converters[ix] = makeJavaConverter(paramTypes[ix])
    }
}