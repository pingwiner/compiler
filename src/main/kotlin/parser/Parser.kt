package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*

class Node() {
    var value: Token? = null
    var subNodes: List<Node>? = null

    constructor(token: Token) : this() {
        value = token
    }

    constructor(nodes: List<Node>) : this() {
        subNodes = nodes
    }
}

class Parser {
    private val operatorPriorityMap = mapOf(
        OperatorType.EQUALS to 0,
        OperatorType.PLUS to 1,
        OperatorType.MINUS to 1,
        OperatorType.MULTIPLY to 2,
        OperatorType.DIVIDE to 2
    )

    fun parse(tokens: List<Token>) {
        var nodes = convertToNodes(tokens)
        nodes = removeBraces(nodes)
        var level = 2
        while (level >= 0) {
            val wrapResult = wrapTokensWithPriority(nodes, level)
            nodes = wrapResult.nodes
            if (!wrapResult.wrapped) {
                level--
            }
        }

        printNodes(nodes)
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

    data class WrapResult(
        val nodes: List<Node>,
        val wrapped: Boolean
    )

    private fun wrapTokensWithPriority(nodes: List<Node>, priority: Int): WrapResult {
        val result = mutableListOf<Node>()
        var i = 0
        var wrapped = false
        while(i < nodes.size) {
            val n = nodes[i]
            if (n.value != null) {
                if (!wrapped) {
                    if (n.value is Operator) {
                        if (nodes.size > 3) {
                            if (operatorPriorityMap[(n.value as Operator).type] == priority) {
                                val subnodes = nodes.subList(i - 1, i + 2)
                                val combined = Node(subnodes)
                                result.removeLast()
                                result.add(combined)
                                i += 2
                                wrapped = true
                                continue
                            }
                        }
                    }
                }
            } else {
                val wrapResult = wrapTokensWithPriority(n.subNodes!!, priority)
                wrapped = wrapped or wrapResult.wrapped
                result.add(Node(wrapResult.nodes))
                i++
                continue
            }
            result.add(n)
            i++
        }
        return WrapResult(result, wrapped)
    }

    private fun removeBraces(nodes: List<Node>): List<Node> {
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

    private fun convertToNodes(tokens: List<Token>): List<Node> {
        val nodes = mutableListOf<Node>()
        for(token in tokens) {
            nodes.add(Node(token))
        }
        return nodes
    }


}