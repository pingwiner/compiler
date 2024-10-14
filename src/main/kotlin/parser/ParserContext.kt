package org.pingwiner.compiler.parser

interface ParserContext {
    val globalVars: List<String>
    val arrays: Map<String, Int>

    fun useFunction(name: String)
}