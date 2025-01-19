package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.Number

val operatorPriorityMap = mapOf(
    Operator.Assign::class to 0,
    Operator.If::class to 1,
    Operator.While::class to 1,
    Operator.Repeat::class to 1,
    Operator.Until::class to 1,
    Operator.Else::class to 2,
    Operator.Or::class to 3,
    Operator.Xor::class to 4,
    Operator.And::class to 4,
    Operator.Eq::class to 5,
    Operator.Neq::class to 5,
    Operator.Lt::class to 6,
    Operator.Gt::class to 6,
    Operator.GtEq::class to 6,
    Operator.LtEq::class to 6,
    Operator.Shl::class to 7,
    Operator.Shr::class to 7,
    Operator.Plus::class to 8,
    Operator.Minus::class to 8,
    Operator.Multiply::class to 9,
    Operator.Divide::class to 9,
    Operator.Mod::class to 9
)

val maxPriorityLevel = operatorPriorityMap.toList().maxByOrNull { (_, value) -> value }!!.second

fun searchForEnd(nodes: List<Node>, start: Int): Int {
    var i = start
    val stack = Stack<Int>()
    while (i < nodes.size) {
        if (nodes[i].value is SpecialSymbol.LCurl) {
            stack.push(1)
        }
        if (nodes[i].value is SpecialSymbol.RCurl) {
            stack.pop()
        }
        if (nodes[i].value is SpecialSymbol.End) {
            if (stack.isEmpty()) return i
        }
        i++
    }
    return i
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

const val NOT_FOUND = -1

inline fun <reified U: SpecialSymbol, reified V: SpecialSymbol> findComplementBraceToken(tokens: List<Token>, start: Int): Int {
    val stack = Stack<Int>()
    var i = start
    while (i < tokens.size) {
        if (tokens[i] is U) {
            stack.push(1)
        }
        if (tokens[i] is V) {
            stack.pop()
            if (stack.isEmpty()) return i
        }
        i++
    }
    return NOT_FOUND
}

inline fun <reified U: SpecialSymbol, reified V: SpecialSymbol> findComplementBraceNode(nodes: List<Node>, start: Int): Int {
    val stack = Stack<Int>()
    var i = start
    while (i < nodes.size) {
        if (nodes[i].value is U) {
            stack.push(1)
        }
        if (nodes[i].value is V) {
            stack.pop()
            if (stack.isEmpty()) return i
        }
        i++
    }
    return NOT_FOUND
}

fun findNextComma(nodes: List<Node>, start: Int): Int {
    val stack = Stack<Int>()
    var i = start
    while (i < nodes.size) {
        when (nodes[i].value) {
            is SpecialSymbol.LBrace -> stack.push(1)
            is SpecialSymbol.RBrace -> stack.pop()
            is SpecialSymbol.Comma -> {
                if (stack.isEmpty()) return i
            }
            else -> {}
        }
        i += 1
    }
    return NOT_FOUND
}

fun unexpectedTokenError(token: Token) {
    throw IllegalArgumentException("Unexpected token " + token.at())
}

fun parseArrayLiteral(tokens: List<Token>) : List<Int> {
    val result = mutableListOf<Int>()
    for (token in tokens) {
        when (token) {
            is Number -> {
                result.add(token.value)
            }
            is SpecialSymbol.Comma -> {}
            else -> {
                unexpectedTokenError(token)
            }
        }
    }
    return result
}

fun parseFunctionArguments(tokens: List<Token>): List<String> {
    val result = mutableListOf<String>()
    for (token in tokens) {
        when (token) {
            is Symbol -> {
                result.add(token.content)
            }
            is SpecialSymbol.Comma -> {}
            else -> {
                unexpectedTokenError(token)
            }
        }
    }
    return result
}