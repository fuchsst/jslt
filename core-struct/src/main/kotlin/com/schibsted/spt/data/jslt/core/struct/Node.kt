package com.schibsted.spt.data.jslt.core.struct

import java.math.BigDecimal
import java.math.BigInteger

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

        companion object {
            fun fromString(str: String): Node.Number {
                val hasDecimalPoint = str.contains('.')
                return if (!hasDecimalPoint) {
                    val int = str.toIntOrNull()
                    if (int != null) {
                        IntNode(int)
                    } else {
                        val long = str.toLongOrNull()
                        if (long != null) {
                            LongNode(long)
                        } else {
                            BigIntNode(BigInteger(str))
                        }
                    }
                } else {
                    val double = str.toDoubleOrNull()
                    if (double != null) {
                        DecimalNode(double)
                    } else {
                        BigDecimalNode(BigDecimal(str))
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
        values.map { (key, value) -> "$key : $value" }.joinToString(separator = ", ", prefix = "{ ", postfix = " }")
}

data class ArrayNode(val values: List<Node>) : Node.Complex() {
    override val isEmpty: Boolean = values.isEmpty()
    override val isNotEmpty: Boolean = values.isNotEmpty()
    override val isArray: Boolean = true
    override fun toString(): String = values.joinToString(separator = ", ", prefix = "[ ", postfix = " ]")
}


data class TextNode(val value: String) : Node.Primitive() {
    override val isTextual: Boolean = true
    override fun toString(): String = value
}


data class IntNode(val value: Int) : Node.Number() {
    override val isIntegral: Boolean = true
    override fun toString(): String = value.toString()
}

data class LongNode(val value: Long) : Node.Number() {
    override val isIntegral: Boolean = true
    override fun toString(): String = value.toString()
}

data class BigIntNode(val value: BigInteger) : Node.Number() {
    override val isIntegral: Boolean = true
    override fun toString(): String = value.toString()
}

data class DecimalNode(val value: Double) : Node.Number() {
    override val isDecimal: Boolean = true
    override fun toString(): String = value.toString()
}

data class BigDecimalNode(val value: BigDecimal) : Node.Number() {
    override val isDecimal: Boolean = true
    override fun toString(): String = value.toString()
}

data class BooleanNode(val value: Boolean) : Node.Constant() {
    override val isBoolean: Boolean = true
    override fun toString(): String = value.toString()
}

data class NullNode(val value: Nothing? = null) : Node.Constant() {
    override val isNull: Boolean = true
    override fun toString(): String = "null"
}
