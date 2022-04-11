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

import com.schibsted.spt.data.jslt.JsltException
import com.schibsted.spt.data.jslt.impl.expressions.LetExpression
import com.schibsted.spt.data.jslt.impl.expressions.VariableExpression
import java.util.*

/**
 * Keeps track of declared variables and maps them to their slots in
 * the stack frames. A stack frame is just an array, with one slot for
 * each variable. There are two kinds of stack frame: the global one,
 * which has top-level variables plus those from the top level of
 * modules. The second type is inside a function.
 *
 *
 * When a variable is declared so that it shadows an outer variable
 * those two get different slots, even though they have the same name.
 *
 *
 * The slot number combines two values in one: which stack frame
 * the variable resolves to, and its position in that frame. The first
 * bit says which frame, and the rest of the bits are left for the
 * slot number.
 *
 *
 * Basically:
 *
 *  * If first bit set: function frame
 *  * If first bit not set: global frame.
 *
 */
class ScopeManager {
    private val globalFrame: StackFrame
    private val scopes: Deque<ScopeFrame?>
    private var functionFrame: StackFrame? = null
    private var functionScopes // null when not in function
            : Deque<ScopeFrame?>? = null

    // when inside function, this is functionScopes else scopes
    private var current: Deque<ScopeFrame?>
    private var currentFrame: StackFrame

    // this is where we track the slots for parameters that must be
    // supplied from the outside
    val parameterSlots: MutableMap<String, Int>

    val stackFrameSize: Int
        get() = currentFrame.nextSlot

    /**
     * Called when we enter a new function. A function is not just a new
     * scope, because it needs its own stack frame.
     */
    fun enterFunction() {
        functionFrame = StackFrame()
        functionScopes = ArrayDeque<ScopeFrame?>()
        current = functionScopes!!
        currentFrame = functionFrame!!
        enterScope()
    }

    fun leaveFunction() {
        functionScopes = null
        current = scopes
        currentFrame = globalFrame
    }

    /**
     * Called when we enter a new lexical scope in which variables can
     * be declared, hiding those declared further out. Although the
     * scopes are nested we flatten them into a single stack frame by
     * simply giving the variables different slots in the same frame.
     * Variable 'v' may map to different slots depending on where in the
     * code it is used.
     */
    fun enterScope() {
        current.push(ScopeFrame(functionScopes != null, currentFrame))
    }

    fun leaveScope() {
        // we don't need this frame anymore (the variables remember their
        // own positions)
        current.pop()
    }

    /**
     * Registers a variable.
     */
    fun registerVariable(let: LetExpression?): VariableInfo {
        val info = LetInfo(let!!)
        current.peek()!!.registerVariable(info)
        return info
    }

    /**
     * Registers a parameter to a function.
     */
    fun registerParameter(parameter: String?, loc: Location?): Int {
        return current.peek()!!.registerVariable(ParameterInfo(parameter!!, loc))
    }

    fun resolveVariable(variable: VariableExpression): VariableInfo {
        val name = variable.variable

        // traversing the scopes from top to bottom
        for (scope in current) {
            val `var` = scope!!.resolveVariable(name)
            if (`var` != null) return `var`
        }

        // might have to traverse global scope, too
        if (functionScopes != null) {
            for (scope in scopes) {
                val `var` = scope!!.resolveVariable(name)
                if (`var` != null) return `var`
            }
        }

        // if we got here it means the variable was not found. that means
        // it's not defined inside the JSLT expression, so it has to be
        // supplied as a parameter from outside during evaluation
        val `var`: VariableInfo = ParameterInfo(name, variable.location)
        val slot = scopes.last!!.registerVariable(`var`)
        parameterSlots[name] = slot
        return `var`
    }

    /**
     * A scope frame is smaller than a stack frame: each object, object
     * comprehension, for expression, and if expression will have its
     * own scope frame. These need to be handled separately because of
     * the shadowing of variables.
     */
    private class ScopeFrame(private val inFunction: Boolean, parent: StackFrame) {
        private val parent: StackFrame
        private val variables: MutableMap<String?, VariableInfo?>
        fun registerVariable(variable: VariableInfo): Int {
            val name = variable.name

            // see if we have a case of duplicate declaration
            if (variables.containsKey(name)) throw JsltException(
                "Duplicate variable declaration " +
                        name, variable.location
            )

            // okay, register this variable
            val level = if (inFunction) 0 else 0x10000000
            val slot = level or parent.nextSlot++ // first free position
            variable.slot = slot
            variables[name] = variable
            return slot
        }

        fun resolveVariable(name: String?): VariableInfo? {
            return variables[name]
        }

        init {
            variables = HashMap<String?, VariableInfo?>()
            this.parent = parent
        }
    }

    private class StackFrame {
        var nextSlot = 0
    }

    companion object {
        const val UNFOUND = -0x1
    }

    init {
        globalFrame = StackFrame()
        scopes = ArrayDeque<ScopeFrame?>()
        current = scopes
        currentFrame = globalFrame
        parameterSlots = HashMap<String, Int>()
    }
}