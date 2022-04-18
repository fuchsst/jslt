package com.schibsted.spt.data.jslt.core.struct

operator fun ObjectNode.minus(other: Node): ObjectNode =
    when (other) {
        is ObjectNode -> ObjectNode(this.values - other.values.keys)
        is ArrayNode -> ObjectNode(this.values - other.values.map { it.toString() }.toSet())
        is TextNode -> ObjectNode(this.values - other.value)
        is NullNode -> this
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")
    }

operator fun ArrayNode.minus(other: Node): ArrayNode =
    when (other) {
        is ObjectNode -> ArrayNode(this.values - other.values.values.toSet())
        is ArrayNode -> ArrayNode(this.values - other.values.toSet())
        is NullNode -> this
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")
    }

operator fun TextNode.minus(other: Node): Node =
    throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")

operator fun IntNode.minus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> IntNode(this.value - other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() - other.value)
        is DoubleNode -> DoubleNode(this.value - other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() - other.value)
        is LongNode -> LongNode(this.value.toLong() - other.value)
        is BooleanNode -> this - IntNode(if (other.value) 1 else 0)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")
    }

operator fun LongNode.minus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> LongNode(this.value - other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() - other.value)
        is DoubleNode -> DoubleNode(this.value - other.value)
        is BigIntNode -> BigIntNode(this.value.toBigInteger() - other.value)
        is LongNode -> LongNode(this.value - other.value)
        is BooleanNode -> this - IntNode(if (other.value) 1 else 0)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")
    }

operator fun BigIntNode.minus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigIntNode(this.value - other.value.toBigInteger())
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() - other.value)
        is DoubleNode -> BigDecimalNode(this.value.toBigDecimal() - other.value.toBigDecimal())
        is BigIntNode -> BigIntNode(this.value - other.value)
        is LongNode -> BigIntNode(this.value - other.value.toBigInteger())
        is BooleanNode -> this - IntNode(if (other.value) 1 else 0)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")
    }

operator fun DoubleNode.minus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> DoubleNode(this.value - other.value)
        is BigDecimalNode -> BigDecimalNode(this.value.toBigDecimal() - other.value)
        is DoubleNode -> DoubleNode(this.value - other.value)
        is BigIntNode -> BigDecimalNode(this.value.toBigDecimal() - other.value.toBigDecimal())
        is LongNode -> DoubleNode(this.value - other.value)
        is BooleanNode -> this - IntNode(if (other.value) 1 else 0)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")
    }

operator fun BigDecimalNode.minus(other: Node): Node.Primitive =
    when (other) {
        is IntNode -> BigDecimalNode(this.value - other.value.toBigDecimal())
        is BigDecimalNode -> BigDecimalNode(this.value - other.value)
        is DoubleNode -> BigDecimalNode(this.value - other.value.toBigDecimal())
        is BigIntNode -> BigDecimalNode(this.value - other.value.toBigDecimal())
        is LongNode -> BigDecimalNode(this.value - other.value.toBigDecimal())
        is BooleanNode -> this - IntNode(if (other.value) 1 else 0)
        is NullNode -> other
        else -> throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")
    }

operator fun BooleanNode.minus(other: Node): BooleanNode =
    throw UnsupportedOperationException("<${this.javaClass.name}> - <${other.javaClass.name}> not allowed.")

operator fun NullNode.minus(other: Node): Node =
    if (other is ArrayNode || other is ObjectNode) {
        other
    } else {
        this
    }
