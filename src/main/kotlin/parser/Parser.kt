package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.codegen.Generator

class Parser {

    fun parse(tokens: List<Token>) {
        val program = Program()
        program.parse(tokens)
        val generator = Generator()
        generator.processNode(program.functions[0].root!!)
        generator.printOperations()
        println("")
    }

}