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
package com.schibsted.spt.data.jslt.impl.operator.comparison

import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.core.struct.Node
import com.schibsted.spt.data.jslt.impl.Location
import com.schibsted.spt.data.jslt.impl.expressions.ExpressionNode
import com.schibsted.spt.data.jslt.impl.operator.AbstractOperator
import com.schibsted.spt.data.jslt.impl.util.number

sealed class ComparisonOperator(
    left: ExpressionNode,
    right: ExpressionNode,
    operator: String,
    location: Location?
) : AbstractOperator(left, right, operator, location)

operator fun Node.compareTo(other: Node): Int {
    return when {
        this.isNumber && other.isNumber -> {
            val n1 = number(this)
            val n2 = number(other)
            if (n1.isIntegralNumber && n2.isIntegralNumber) // if both are integers, then compare them as such
                n1.longValue().compareTo(n2.longValue())
            else
                n1.doubleValue().compareTo(n2.doubleValue())
        }
        this.isTextual && other.isTextual -> {
            val s1 = this.asText()
            val s2 = other.asText()
            s1.compareTo(s2)
        }
        this.isNull || other.isNull -> {
            // null is equal to itself, and considered the smallest of all
            if (this.isNull && other.isNull)
                0
            else if (this.isNull)
                -1
            else
                1
        }
        else -> throw JsltException("Can't compare $this and $other")
    }
}

