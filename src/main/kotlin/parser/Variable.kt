package org.pingwiner.compiler.parser

class Variable(val name: String) {
    var size: Int = 1
    var value: List<Int>? = null

    fun isArray() = size > 1
}