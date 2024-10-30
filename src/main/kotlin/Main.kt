package org.pingwiner.compiler

import org.pingwiner.compiler.codegen.Generator
import org.pingwiner.compiler.parser.Parser

fun main() {
    val reader = Reader("input.txt")
    val lexer = Lexer()
    lexer.scan(reader.content)
    //lexer.printTokens()
    val parser = Parser()
    val program = parser.parse(lexer.tokens)
    val generator = Generator(program)
    generator.generate()
    generator.printOperations()
    println("")
}