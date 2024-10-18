package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.Number

val operatorPriorityMap = mapOf(
    OperatorType.ASSIGN to 0,
    OperatorType.IF to 1,
    OperatorType.WHILE to 1,
    OperatorType.REPEAT to 1,
    OperatorType.UNTIL to 1,
    OperatorType.ELSE to 2,
    OperatorType.ORL to 3,
    OperatorType.ANDL to 4,
    OperatorType.ORB to 5,
    OperatorType.XOR to 6,
    OperatorType.ANDB to 6,
    OperatorType.EQ to 7,
    OperatorType.NEQ to 7,
    OperatorType.LT to 8,
    OperatorType.GT to 8,
    OperatorType.GTEQ to 8,
    OperatorType.LTEQ to 8,
    OperatorType.LTEQ to 8,
    OperatorType.SHL to 9,
    OperatorType.SHR to 9,
    OperatorType.PLUS to 10,
    OperatorType.MINUS to 10,
    OperatorType.MULTIPLY to 11,
    OperatorType.DIVIDE to 11
)

val maxPriorityLevel = operatorPriorityMap.toList().maxByOrNull { (_, value) -> value }!!.second

fun searchForEnd(nodes: List<Node>, start: Int): Int {
    var i = start
    val stack = Stack<Int>()
    while (i < nodes.size) {
        if (nodes[i].value?.tokenType == TokenType.L_CURL) {
            stack.push(1)
        }
        if (nodes[i].value?.tokenType == TokenType.R_CURL) {
            stack.pop()
        }
        if (nodes[i].value?.tokenType == TokenType.END) {
            if (stack.isEmpty()) return i
        }
        i++
    }
    return i
}

fun findRBrace(nodes: List<Node>, start: Int): Int {
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

fun printNodes(nodes: List<Node>, level: Int = 0): String {
    var padding = ""
    val sb = StringBuilder()
    for (i in 0..level) {
        padding += "  "
    }
    for (n in nodes) {
        n.value?.let {
            //println(padding + it)
            sb.append(padding + it)
        }
        n.subNodes?.let {
            //println("$padding{")
            sb.append("$padding{")
            sb.append(printNodes(it, level + 1))
            //println("$padding}")
            sb.append("$padding}")
        }
    }
    return sb.toString()
}

fun convertToNodes(tokens: List<Token>): List<Node> {
    val nodes = mutableListOf<Node>()
    for(token in tokens) {
        nodes.add(Node(token))
    }
    return nodes
}

fun findLastCurlBrace(tokens: List<Token>, start: Int): Int {
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

fun findComplementBrace(nodes: List<Node>, start: Int, braceType: TokenType): Int {
    val complement = mapOf(
        TokenType.L_BRACE to TokenType.R_BRACE,
        TokenType.L_SQUARE to TokenType.R_SQUARE,
        TokenType.L_CURL to TokenType.R_CURL
    )
    if (!complement.contains(braceType)) return -1
    val stack = Stack<Int>()
    var i = start
    while (i < nodes.size) {
        if (nodes[i].value?.tokenType == braceType) {
            stack.push(1)
        }
        if (nodes[i].value?.tokenType == complement[braceType]) {
            stack.pop()
            if (stack.isEmpty()) return i
        }
        i++
    }
    return -1
}

fun findNextComma(nodes: List<Node>, start: Int): Int {
    val stack = Stack<Int>()
    var i = start
    while (i < nodes.size) {
        when (nodes[i].value?.tokenType) {
            TokenType.L_BRACE -> stack.push(1)
            TokenType.R_BRACE -> stack.pop()
            TokenType.COMMA -> {
                if (stack.isEmpty()) return i
            }
            else -> {}
        }
        i += 1
    }
    return -1
}

fun unexpectedTokenError(token: Token) {
    throw IllegalArgumentException("Unexpected token " + token.at())
}

fun parseArrayLiteral(tokens: List<Token>) : List<Int> {
    val result = mutableListOf<Int>()
    for (token in tokens) {
        if (token is Number) {
            result.add(token.value)
        }
    }
    return result
}
