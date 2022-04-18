package com.schibsted.spt.data.jslt.core.struct

operator fun ObjectNode.times(other: Node): ObjectNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")

operator fun ArrayNode.times(other: Node): ArrayNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")

operator fun TextNode.times(other: Node): TextNode =
    when (other) {
        is IntNode -> TextNode(this.value.repeat(other.value))
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")
    }

operator fun IntNode.times(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> IntNode(this.value * other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() * other.value)
        is DoubleNode -> DoubleNode(this.value * other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() * other.value)
        is LongNode -> LongNode(this.value.toLong() * other.value)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")
    }

operator fun LongNode.times(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> LongNode(this.value * other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() * other.value)
        is DoubleNode -> DoubleNode(this.value * other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() * other.value)
        is LongNode -> LongNode(this.value * other.value)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")
    }

operator fun BigIntNode.times(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigIntNode(this.value * other.value.toBigInteger())
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() * other.value)
        is DoubleNode -> BigDecimalNode(this.value.toBigDecimal() * other.value.toBigDecimal())
        is BigIntNode -> BigIntNode(this.value * other.value)
        is LongNode -> BigIntNode(this.value * other.value.toBigInteger())
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")
    }

operator fun DoubleNode.times(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> DoubleNode(this.value * other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() * other.value)
        is DoubleNode -> DoubleNode(this.value * other.value)
        is BigIntNode -> BigDecimalNode(this.value.toBigDecimal() * other.value.toBigDecimal())
        is LongNode -> DoubleNode(this.value * other.value)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")
    }

operator fun BigDecimalNode.times(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigDecimalNode(this.value * other.value.toBigDecimal())
        is BigDecimalNode -> BigDecimalNode(this.value * other.value)
        is DoubleNode -> BigDecimalNode(this.value * other.value.toBigDecimal())
        is BigIntNode -> BigDecimalNode(this.value * other.value.toBigDecimal())
        is LongNode -> BigDecimalNode(this.value * other.value.toBigDecimal())
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")
    }

operator fun BooleanNode.times(other: Node): BooleanNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> * <${other.javaClass.name}> not allowed.")

@Suppress("UNUSED_PARAMETER")
operator fun NullNode.times(other: Node): Node =
    this
    
