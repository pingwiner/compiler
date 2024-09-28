package org.pingwiner.compiler.parser

import org.pingwiner.compiler.Token

class Function(val name: String, val params: List<String>) {
    val mainBlock: Block? = null

    fun parse(tokens: List<Token>) {

    }
}
