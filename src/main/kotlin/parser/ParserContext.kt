package org.pingwiner.compiler.parser

interface ParserContext {
    fun useFunction(name: String)
    fun hasVariable(name: String): Boolean
    fun getConstant(name: String): Int?
    fun hasArray(name: String): Boolean
    fun arraySize(name: String): Int
}