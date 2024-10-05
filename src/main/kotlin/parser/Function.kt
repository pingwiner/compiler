package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.Number

class Function(val name: String, val params: List<String>) {
    private var globalVars = listOf<String>()
    val vars = mutableListOf<String>()
    val statements = mutableListOf<Statement>()

    open class Statement(
        val node: ASTNode
    )

    fun parse(tokens: List<Token>, globalVars: List<String>) {
        this.globalVars = globalVars
        parseBlock(tokens)
    }

    private fun parseBlock(tokens: List<Token>) {
        var i = 0
        while (i < tokens.size) {
            val end = searchForEnd(tokens, i)
            val statement = parseStatement(tokens.subList(i, end))
            i = end + 1
            statements.add(statement)
        }
    }

    private fun parseStatement(tokens: List<Token>): Statement {
        return parseStatementNodes(convertToNodes(tokens))
    }

    private fun parseStatementNodes(inputNodes: List<Node>): Statement {
        var nodes = wrapFunctionCalls(inputNodes)
        nodes = removeBraces(nodes)

        var level = maxPriorityLevel
        while (level >= 0) {
            val wrapResult = wrapTokensWithPriority(nodes, level)
            nodes = wrapResult.nodes
            if (!wrapResult.wrapped) {
                level--
            }
        }
        return Statement(nodesToAst(nodes))
    }

    private fun wrapFunctionCalls(nodes: List<Node>): List<Node>  {
        var i = 0
        val result = mutableListOf<Node>()
        while (i < nodes.size) {
            if (nodes[i].value?.tokenType == TokenType.L_BRACE && (i > 0) && nodes[i - 1].value?.tokenType == TokenType.SYMBOL) {
                val j = findLastBrace(nodes, i)
                result.last().subNodes = nodes.subList(i + 1, j)
                result.last().isFunction = true
                i = j + 1
            } else {
                result.add(nodes[i])
                i += 1
            }
        }
        return result
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

    private fun nodesToAst(nodes: List<Node>): ASTNode {
        if (nodes.size == 1) {
            return nodeToAstNode(nodes[0])
        } else if (nodes.size == 3) {
            if (nodes[1].value?.tokenType == TokenType.OPERATOR) {
                val left = nodeToAstNode(nodes[0])
                val right = nodeToAstNode(nodes[2])
                return nodeToAstNode(nodes[1], left, right)
            } else {
                throw IllegalArgumentException("Unexpected token " + nodes[1].value?.at())
            }
        } else {
            throw IllegalArgumentException("Syntax error at node " + nodes[0].toString())
        }
    }

    private fun nodeToAstNode(node: Node): ASTNode {
        return when(node.value?.tokenType) {
            TokenType.NUMBER -> ASTNode.ImmediateValue((node.value as Number).value)
            TokenType.SYMBOL -> {
                val varName = (node.value as Symbol).content
                if (!node.isFunction) {
                    if (!globalVars.contains(varName)) {
                        if (!params.contains(varName)) {
                            if (!vars.contains(varName)) {
                                vars.add(varName)
                            }
                        }
                    }
                    ASTNode.Variable(varName)
                } else {
                    val arguments = parseFunctionArguments(node.subNodes ?: listOf())
                    ASTNode.FunctionCall(varName, arguments)
                }
            }
            TokenType.KEYWORD -> {
                if ((node.value as Keyword).type == KeywordType.RESULT) {
                    ASTNode.Result()
                } else {
                    throw IllegalArgumentException("Syntax error " + node.value?.at())
                }
            }
            null -> {
                if (node.subNodes != null) {
                    nodesToAst(node.subNodes!!)
                } else {
                    throw IllegalArgumentException("Bad token " + node.value?.at())
                }
            }
            else -> { throw IllegalArgumentException("Syntax error " + node.value?.at())}
        }
    }

    private fun parseFunctionArguments(nodes: List<Node>): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        if (nodes.isEmpty()) return result
        var i = 0
        while (i < nodes.size) {
            var j = findNextComma(nodes, i)
            if (j == -1) {
                j = nodes.size
            }
            val statement = parseStatementNodes(nodes.subList(i, j))
            result.add(statement.node)
            i = j + 1
        }
        return result
    }

    private fun nodeToAstNode(node: Node, left: ASTNode, right: ASTNode): ASTNode {
        val op = node.value as Operator
        return when(op.type) {
            OperatorType.PLUS -> ASTNode.Plus(left, right)
            OperatorType.MINUS -> ASTNode.Minus(left, right)
            OperatorType.MULTIPLY -> ASTNode.Multiply(left, right)
            OperatorType.DIVIDE -> ASTNode.Divide(left, right)
            OperatorType.ASSIGN -> {
                if (left is ASTNode.Variable || left is ASTNode.Result) {
                    ASTNode.Assign(left, right)
                } else {
                    throw IllegalArgumentException("Illegal assignment " + node.value?.at())
                }
            }
            OperatorType.IF -> ASTNode.If(left, right)
            OperatorType.EQ -> ASTNode.Eq(left, right)
            OperatorType.LT -> ASTNode.Lt(left, right)
            OperatorType.GT -> ASTNode.Gt(left, right)
            OperatorType.GTEQ -> ASTNode.GtEq(left, right)
            OperatorType.LTEQ -> ASTNode.LtEq(left, right)
        }
    }

}
