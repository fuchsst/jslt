package com.schibsted.spt.data.jslt.core.struct

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

sealed class Node {
    open val isObject: Boolean = false
    open val isArray: Boolean = false

    open val isPrimitive: Boolean = false

    open val isTextual: Boolean = false

    open val isNumeric: Boolean = false

    open val isIntegral: Boolean = false
    open val isDecimal: Boolean = false

    open val isConstant: Boolean = false
    open val isBoolean: Boolean = false
    open val isNull: Boolean = false

    sealed class Complex : Node() {
        abstract val isEmpty: Boolean
        abstract val isNotEmpty: Boolean
    }

    sealed class Primitive : Node() {
        override val isPrimitive: Boolean = true
    }

    sealed class Number : Primitive() {
        override val isNumeric: Boolean = true

        sealed class Integral : Number() {
            override val isIntegral: Boolean = true

            companion object {
                fun fromString(str: String): Number {
                    val int = str.toIntOrNull()
                    return if (int != null) {
                        IntNode(int)
                    } else {
                        val long = str.toLongOrNull()
                        if (long != null) {
                            LongNode(long)
                        } else {
                            BigIntNode(BigInteger(str))
                        }
                    }
                }
            }
        }

        sealed class Decimal : Number() {
            override val isDecimal: Boolean = true

            companion object {
                fun fromString(decimal: String, exponent: String? = null): Number {
                    return if (exponent == null) {
                        // in most cases it will be in the range of a double anyway,
                        // so we try to convert it and do it twice if it fails
                        val double = decimal.toDoubleOrNull()
                        if (double != null) {
                            DoubleNode(double)
                        } else {
                            BigDecimalNode(BigDecimal(decimal))
                        }
                    } else {
                        val decimalAsDouble = decimal.toDouble()
                        val exponentAsInt = exponent.toInt()
                        if (-308 <= exponentAsInt && exponentAsInt <= 308) {
                            DoubleNode(decimalAsDouble * 10.0.pow(exponent.toDouble()))
                        } else {
                            println(decimal)
                            println(exponent)
                            BigDecimalNode(BigDecimal(decimal) * BigDecimal(10).pow(exponentAsInt))
                        }
                    }
                }
            }
        }
    }

    sealed class Constant : Primitive() {
        override val isConstant: Boolean = true
    }
}

data class ObjectNode(val values: Map<String, Node>) : Node.Complex() {
    override val isEmpty: Boolean = values.isEmpty()
    override val isNotEmpty: Boolean = values.isNotEmpty()
    override val isObject: Boolean = true
    override fun toString(): String =
        values.map { (key, value) -> "\"$key\":$value" }.joinToString(separator = ",", prefix = "{", postfix = "}")
}

data class ArrayNode(val values: List<Node>) : Node.Complex() {
    override val isEmpty: Boolean = values.isEmpty()
    override val isNotEmpty: Boolean = values.isNotEmpty()
    override val isArray: Boolean = true
    override fun toString(): String = values.joinToString(separator = ",", prefix = "[", postfix = "]")
}


data class TextNode(val value: String) : Node.Primitive() {
    override val isTextual: Boolean = true
    override fun toString(): String = "\"$value\""
}


data class IntNode(val value: Int) : Node.Number.Integral() {
    override fun toString(): String = value.toString()
}

data class LongNode(val value: Long) : Node.Number.Integral() {
    override fun toString(): String = value.toString()
}

data class BigIntNode(val value: BigInteger) : Node.Number.Integral() {
    override fun toString(): String = value.toString()
}

data class DoubleNode(val value: Double) : Node.Number.Decimal() {
    override val isDecimal: Boolean = true
    override fun toString(): String = value.toString()
}

data class BigDecimalNode(val value: BigDecimal) : Node.Number.Decimal() {
    override val isDecimal: Boolean = true
    override fun toString(): String = value.toString()
}

data class BooleanNode(val value: Boolean) : Node.Constant() {
    companion object {
        val TRUE = BooleanNode(true)
        val FALSE = BooleanNode(false)
    }
    override val isBoolean: Boolean = true
    override fun toString(): String = value.toString()
}

data class NullNode(val value: Nothing? = null) : Node.Constant() {
    companion object {
        val instance = NullNode()
    }
    override val isNull: Boolean = true
    override fun toString(): String = "null"
}
