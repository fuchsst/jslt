package com.schibsted.spt.data.jslt.core.struct

operator fun ObjectNode.plus(other: Node): Node =
    when (other) {
        // order is relevant, "this" second means keys in that map have precedence and their values are returned if the key is present in both maps
        is ObjectNode -> ObjectNode(other.values + this.values)
        is TextNode -> TextNode(this.toString() + other)
        is NullNode -> this
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")
    }

operator fun ArrayNode.plus(other: Node): Node =
    when (other) {
        is ArrayNode -> ArrayNode(this.values + other.values)
        is TextNode -> TextNode(this.toString() + other)
        is NullNode -> this
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")
    }

operator fun TextNode.plus(other: Node): Node =
    when (other) {
        is NullNode -> this
        is TextNode -> TextNode(this.value + other.value)
        else -> TextNode(this.value + other.toString())
    }

operator fun IntNode.plus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> IntNode(this.value + other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() + other.value)
        is DoubleNode -> DoubleNode(this.value + other.value)
        is LongNode -> LongNode(this.value + other.value)
        is TextNode -> TextNode(this.toString() + other)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")
    }

operator fun LongNode.plus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> LongNode(this.value + other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() + other.value)
        is DoubleNode -> DoubleNode(this.value + other.value)
        is LongNode -> LongNode(this.value + other.value)
        is TextNode -> TextNode(this.toString() + other)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")
    }

operator fun BigIntNode.plus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigIntNode(this.value + other.value.toBigInteger())
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
        is BigIntNode -> BigIntNode(this.value + other.value)
        is DoubleNode -> BigDecimalNode(this.value.toBigDecimal() + other.value.toBigDecimal())
        is LongNode -> BigIntNode(this.value + other.value.toBigInteger())
        is TextNode -> TextNode(this.toString() + other)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")
    }

operator fun DoubleNode.plus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> DoubleNode(this.value + other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
        is BigIntNode -> BigDecimalNode(this.value.toBigDecimal() + other.value.toBigDecimal())
        is DoubleNode -> DoubleNode(this.value + other.value)
        is LongNode -> DoubleNode(this.value + other.value)
        is TextNode -> TextNode(this.toString() + other)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")
    }

operator fun BigDecimalNode.plus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
        is BigDecimalNode -> BigDecimalNode(this.value + other.value)
        is BigIntNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
        is DoubleNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
        is LongNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
        is TextNode -> TextNode(this.toString() + other)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")
    }

operator fun BooleanNode.plus(other: Node): BooleanNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> + <${other.javaClass.name}> not allowed.")

operator fun NullNode.plus(other: Node): Node =
    if (other is ArrayNode || other is ObjectNode) {
        other
    } else {
        this
    }

