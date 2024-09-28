package org.pingwiner.compiler.parser

class Block {
    var variables = listOf<String>()
    var statements = mutableListOf<Statement>()
}