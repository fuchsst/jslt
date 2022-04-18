package com.schibsted.spt.data.jslt.core.struct

operator fun ObjectNode.div(other: Node): ObjectNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")

operator fun ArrayNode.div(other: Node): ArrayNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")

operator fun TextNode.div(other: Node): Node =
    throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")

operator fun IntNode.div(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> IntNode(this.value / other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() / other.value)
        is DoubleNode -> DoubleNode(this.value / other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() / other.value)
        is LongNode -> LongNode(this.value.toLong() / other.value)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")
    }

operator fun LongNode.div(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> LongNode(this.value / other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() / other.value)
        is DoubleNode -> DoubleNode(this.value / other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() / other.value)
        is LongNode -> LongNode(this.value / other.value)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")
    }

operator fun BigIntNode.div(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigIntNode(this.value / other.value.toBigInteger())
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() / other.value)
        is DoubleNode -> BigDecimalNode(this.value.toBigDecimal() / other.value.toBigDecimal())
        is BigIntNode -> BigIntNode(this.value / other.value)
        is LongNode -> BigIntNode(this.value / other.value.toBigInteger())
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")
    }

operator fun DoubleNode.div(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> DoubleNode(this.value / other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() / other.value)
        is DoubleNode -> DoubleNode(this.value / other.value)
        is BigIntNode -> BigDecimalNode(this.value.toBigDecimal() / other.value.toBigDecimal())
        is LongNode -> DoubleNode(this.value / other.value)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")
    }

operator fun BigDecimalNode.div(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigDecimalNode(this.value / other.value.toBigDecimal())
        is BigDecimalNode -> BigDecimalNode(this.value / other.value)
        is DoubleNode -> BigDecimalNode(this.value / other.value.toBigDecimal())
        is BigIntNode -> BigDecimalNode(this.value / other.value.toBigDecimal())
        is LongNode -> BigDecimalNode(this.value / other.value.toBigDecimal())
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")
    }

operator fun BooleanNode.div(other: Node): BooleanNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> / <${other.javaClass.name}> not allowed.")

@Suppress("UNUSED_PARAMETER")
operator fun NullNode.div(other: Node): Node =
    this
    
