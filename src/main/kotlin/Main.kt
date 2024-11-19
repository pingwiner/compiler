package org.pingwiner.compiler

import org.pingwiner.compiler.codegen.IrGenerator
import org.pingwiner.compiler.parser.Parser

fun main() {
    val reader = Reader("input.txt")
    val lexer = Lexer()
    lexer.scan(reader.content)
    //lexer.printTokens()
    val parser = Parser()
    val program = parser.parse(lexer.tokens)
    val irGenerator = IrGenerator(program)
    irGenerator.generate()
    println("")
}