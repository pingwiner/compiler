package org.pingwiner.compiler

import org.pingwiner.compiler.parser.Parser

fun main() {
    val reader = Reader("input.txt")
    val lexer = Lexer()
    lexer.scan(reader.content)
    //lexer.printTokens()
    val parser = Parser()
    parser.parse(lexer.tokens)
}