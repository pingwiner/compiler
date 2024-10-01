package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*

class Function(val name: String, val params: List<String>) {
    val mainBlock: Block? = null
    val vars = mutableListOf<String>()
    val statements = mutableListOf<Statement>()
    var result: Return? = null

    open class Statement(
        val nodes: List<Node>
    )

    class Return(val statement: Statement) {

    }

    fun parse(tokens: List<Token>) {
        parseBlock(tokens)
    }

    private fun parseBlock(tokens: List<Token>) {
        var i = 0
        while (i < tokens.size) {
            if (tokens[i] is Keyword) {
                when((tokens[i] as Keyword).type) {
                    KeywordType.VAR -> {
                        i = parseVarDefinition(tokens, i)
                    }

                    KeywordType.RETURN -> {
                        val end = searchForEnd(tokens, i)
                        val statement = parseStatement(tokens.subList(i + 1, end))
                        i = end + 1
                        result = Return(statement)
                    }
                    KeywordType.FUN -> TODO()
                    KeywordType.IF -> TODO()
                    KeywordType.ELSE -> TODO()
                    KeywordType.WHILE -> TODO()
                    KeywordType.REPEAT -> TODO()
                }
            } else {
                val end = searchForEnd(tokens, i)
                val statement = parseStatement(tokens.subList(i, end))
                i = end + 1
                statements.add(statement)
            }
        }
    }

    private fun parseStatement(tokens: List<Token>): Statement {
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
        return Statement(nodes)
    }

    private fun parseVarDefinition(tokens: List<Token>, start: Int): Int {
        if (tokens[start + 1].tokenType != TokenType.SYMBOL) {
            unexpectedTokenError(tokens[start + 1])
        }
        if (tokens[start + 2].tokenType != TokenType.END) {
            unexpectedTokenError(tokens[start + 2])
        }
        vars.add((tokens[start + 1] as Symbol).content)
        return start + 3
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


}
