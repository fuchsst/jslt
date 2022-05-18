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

import com.schibsted.spt.data.jslt.core.struct.Node
import java.util.*

open class Scope(stackFrameSize: Int) {
    private val globalStackFrame: Array<Node?> = arrayOfNulls(stackFrameSize)
    private val localStackFrames: ArrayDeque<Array<Node?>> = ArrayDeque()
    fun enterFunction(stackFrameSize: Int) {
        localStackFrames.push(arrayOfNulls(stackFrameSize))
    }

    fun leaveFunction() {
        localStackFrames.pop()
    }

    fun getValue(slot: Int): Node? {
        return if (slot and BITMASK != 0) globalStackFrame[slot and INVERSE] else localStackFrames.peek()[slot]
    }

    open fun setValue(slot: Int, value: Node) {
        if (slot and BITMASK != 0) globalStackFrame[slot and INVERSE] = value else localStackFrames.peek()[slot] = value
    }

    companion object {
        fun getRoot(stackFrameSize: Int): Scope {
            return Scope(stackFrameSize)
        }

        /**
         * Creates an initialized scope with values for variables supplied
         * by client code into the JSLT expression.
         */
        fun makeScope(
            variables: Map<String, Node>,
            stackFrameSize: Int,
            parameterSlots: Map<String, Int>,
        ): Scope {
            val scope = Scope(stackFrameSize)

            variables.forEach { (key, variable) ->
                if (parameterSlots.containsKey(key))
                    scope.setValue(parameterSlots[key]!!, variable)
            }

            return scope
        }

        private const val BITMASK = 0x10000000
        private const val INVERSE = -0x10000001
    }

}