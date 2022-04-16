package com.schibsted.spt.data.jslt.core.struct

import java.io.InputStream

interface StructConverter {

    fun asStruct():Node
}