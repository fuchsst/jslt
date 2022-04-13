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
package com.schibsted.spt.data.jslt

import com.schibsted.spt.data.jslt.impl.Location

/**
 * Parent exception for all exceptions thrown by JSLT for both
 * compilation and run-time errors.
 */
class JsltException @JvmOverloads constructor(
    message: String?,
    cause: Throwable? = null,
    private val location: Location? = null
) : RuntimeException(message, cause) {
    constructor(message: String?, location: Location?) : this(message, null, location)

    /**
     * Returns the error message with location information.
     */
    override val message: String
        get() = if (location != null) "${super.message}  at $location" else super.message!!

    /**
     * Returns the error message without location information.
     */
    fun getMessageWithoutLocation(): String? {
        return super.message
    }

    /**
     * What file/resource did the error occur in? Can be null.
     */
    fun getSource(): String? {
        return location?.source
    }

    /**
     * What line did the error occur on? -1 if unknown.
     */
    fun getLine(): Int {
        return location?.line ?: -1
    }

    /**
     * What column did the error occur on? -1 if unknown.
     */
    fun getColumn(): Int {
        return location?.column ?: -1
    }
}