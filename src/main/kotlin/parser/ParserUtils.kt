package org.pingwiner.compiler.parser

import org.pingwiner.compiler.OperatorType
import org.pingwiner.compiler.Token
import org.pingwiner.compiler.TokenType

val operatorPriorityMap = mapOf(
    OperatorType.EQUALS to 0,
    OperatorType.PLUS to 1,
    OperatorType.MINUS to 1,
    OperatorType.MULTIPLY to 2,
    OperatorType.DIVIDE to 2
)

fun removeBraces(nodes: List<Node>): List<Node> {
    val result = mutableListOf<Node>()
    var i = 0
    while(i < nodes.size) {
        val n = nodes[i]
        if (n.value!!.tokenType == TokenType.L_BRACE) {
            val j = findRBrace(nodes, i)
            if (j == -1) {
                throw IllegalStateException("Unbalanced braces")
            }
            val subnodes = removeBraces(nodes.subList(i + 1, j))
            if (subnodes.isEmpty()) {
                //do nothing
            } else if (subnodes.size == 1) {
                result.add(subnodes[0])
            } else {
                val combined = Node(subnodes)
                result.add(combined)
            }
            i = j + 1
        } else {
            result.add(n)
            i += 1
        }
    }
    return result
}

fun searchForEnd(tokens: List<Token>, start: Int): Int {
    var i = start
    while (i < tokens.size) {
        if (tokens[i].tokenType == TokenType.END) return i
        i++
    }
    return i
}

private fun findRBrace(nodes: List<Node>, start: Int): Int {
    val stack = Stack<Int>()
    for (i in start..< nodes.size) {
        if (nodes[i].value!!.tokenType == TokenType.L_BRACE) {
            stack.push(1)
        } else if (nodes[i].value!!.tokenType == TokenType.R_BRACE) {
            stack.pop()
            if (stack.isEmpty()) {
                return i
            }
        }
    }
    return -1
}

private fun printNodes(nodes: List<Node>, level: Int = 0) {
    var padding = ""
    for (i in 0..level) {
        padding += "  "
    }
    for (n in nodes) {
        n.value?.let {
            println(padding + it)
        }
        n.subNodes?.let {
            println("$padding{")
            printNodes(it, level + 1)
            println("$padding}")
        }
    }
}

fun convertToNodes(tokens: List<Token>): List<Node> {
    val nodes = mutableListOf<Node>()
    for(token in tokens) {
        nodes.add(Node(token))
    }
    return nodes
}

fun findLastBrace(tokens: List<Token>, start: Int): Int {
    val stack = Stack<Int>()
    var i = start
    while (i < tokens.size) {
        if (tokens[i].tokenType == TokenType.L_CURL) {
            stack.push(1)
        }
        if (tokens[i].tokenType == TokenType.R_CURL) {
            stack.pop()
            if (stack.isEmpty()) return i
        }
        i++
    }
    return -1
}

fun unexpectedTokenError(token: Token) {
    throw IllegalArgumentException("Unexpected token at line " + token.line + ", position " + token.position)
}