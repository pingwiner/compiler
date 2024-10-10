package org.pingwiner.compiler.parser

interface ParserContext {
    val globalVars: List<String>

    fun useFunction(name: String)
}