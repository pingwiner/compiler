package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*

class Parser {

    fun parse(tokens: List<Token>) {
        val program = Program()
        program.parse(tokens)
        println("")
    }

}