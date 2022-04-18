package com.schibsted.spt.data.jslt.core.struct


operator fun TextNode.plus(other: Node): TextNode =
    when (other) {
        is NullNode -> {
            this
        }
        is TextNode -> {
            TextNode(this.value + other.value)
        }
        else -> {
            TextNode(this.value + other.toString())
        }
    }

operator fun ArrayNode.plus(other: TextNode): TextNode = TextNode(this.toString() + other)

operator fun ArrayNode.plus(other: ArrayNode): ArrayNode = ArrayNode(this.values + other.values)

@Suppress("UNUSED_PARAMETER")
operator fun ArrayNode.plus(other: NullNode): ArrayNode = this

operator fun ObjectNode.plus(other: TextNode): TextNode = TextNode(this.toString() + other)

operator fun ObjectNode.plus(other: ObjectNode): ObjectNode =
// order is relevant, "this" second means keys in that map have precedence
    // and their values are returned if the key is present in both maps
    ObjectNode(other.values + this.values)

@Suppress("UNUSED_PARAMETER")
operator fun ObjectNode.plus(other: NullNode): ObjectNode = this

operator fun NullNode.plus(other: Node): Node =
    if (other is ArrayNode || other is ObjectNode) {
        other
    } else {
        this
    }

operator fun Node.Number.plus(other: NullNode): Node = other

operator fun Node.Number.plus(other: BooleanNode): Node.Number = this + IntNode(if (other.value) 1 else 0)

operator fun Node.Number.plus(other: TextNode): Node = TextNode(this.toString() + other)

operator fun Node.Number.plus(other: Node.Number): Node.Number {
    return when (this) {
        is IntNode -> {
            when (other) {
                is IntNode -> IntNode(this.value + other.value)
                is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
                is BigIntNode -> BigIntNode(this.value.toBigInteger() + other.value)
                is DoubleNode -> DoubleNode(this.value + other.value)
                is LongNode -> LongNode(this.value + other.value)
            }
        }
        is BigDecimalNode -> {
            when (other) {
                is IntNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
                is BigDecimalNode -> BigDecimalNode(this.value + other.value)
                is BigIntNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
                is DoubleNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
                is LongNode -> BigDecimalNode(this.value + other.value.toBigDecimal())
            }
        }
        is BigIntNode -> {
            when (other) {
                is IntNode -> BigIntNode(this.value + other.value.toBigInteger())
                is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
                is BigIntNode -> BigIntNode(this.value + other.value)
                is DoubleNode -> BigDecimalNode(this.value.toBigDecimal() + other.value.toBigDecimal())
                is LongNode -> BigIntNode(this.value + other.value.toBigInteger())
            }
        }
        is DoubleNode -> {
            when (other) {
                is IntNode -> DoubleNode(this.value + other.value)
                is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
                is BigIntNode -> BigDecimalNode(this.value.toBigDecimal() + other.value.toBigDecimal())
                is DoubleNode -> DoubleNode(this.value + other.value)
                is LongNode -> DoubleNode(this.value + other.value)
            }
        }
        is LongNode -> {
            when (other) {
                is IntNode -> LongNode(this.value + other.value)
                is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() + other.value)
                is BigIntNode -> BigIntNode(this.value.toBigInteger() + other.value)
                is DoubleNode -> DoubleNode(this.value + other.value)
                is LongNode -> LongNode(this.value + other.value)
            }
        }
    }
}
