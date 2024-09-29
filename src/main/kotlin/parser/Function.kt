package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*

class Function(val name: String, val params: List<String>) {
    val mainBlock: Block? = null

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

                    KeywordType.RETURN -> TODO()
                    KeywordType.FUN -> TODO()
                    KeywordType.IF -> TODO()
                    KeywordType.ELSE -> TODO()
                    KeywordType.WHILE -> TODO()
                    KeywordType.REPEAT -> TODO()
                }
            } else {
                val end = searchForEnd(tokens, i)
                parseStatement(tokens.subList(i, end))
                i = end + 1
            }
        }
    }

    private fun parseStatement(tokens: List<Token>) {
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

        //printNodes(nodes)
    }

    private fun parseVarDefinition(tokens: List<Token>, start: Int): Int {
        TODO()
        return start
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
