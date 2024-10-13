package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.Number

class Function(val name: String, val params: List<String>) {
    private lateinit var context: ParserContext
    val vars = mutableListOf<String>()
    val statements = mutableListOf<Statement>()

    override fun toString(): String {
        val sb = StringBuilder()
        var i = 0
        for (arg in params) {
            sb.append(arg)
            i += 1
            if (i < params.size) {
                sb.append(", ")
            }
        }
        return "$name($sb)"
    }

    open class Statement(
        val node: ASTNode
    )

    fun parse(tokens: List<Token>, context: ParserContext) {
        this.context = context
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

    private fun isVariableExists(name: String): Boolean {
        if (!context.globalVars.contains(name)) {
            if (!params.contains(name)) {
                if (!vars.contains(name)) {
                    return false
                }
            }
        }
        return true
    }

    private fun nodesToAst(nodes: List<Node>): ASTNode {
        if (nodes.size == 1) {
            return nodeToAstNode(nodes[0])
        } else if (nodes.size == 3) {
            if (nodes[1].value?.tokenType == TokenType.OPERATOR) {
                val left = nodeToAstNode(nodes[0], false)
                if (left is ASTNode.Variable) {
                    val name = left.name
                    if (!isVariableExists(name)) {
                        vars.add(name)
                    }
                }
                val right = nodeToAstNode(nodes[2])
                return nodeToAstNode(nodes[1], left, right)
            } else {
                throw IllegalArgumentException("Unexpected token " + nodes[1].value?.at())
            }
        } else {
            throw IllegalArgumentException("Syntax error at node " + nodes[0].toString())
        }
    }

    private fun nodeToAstNode(node: Node, checkVariableExistence: Boolean = true): ASTNode {
        return when(node.value?.tokenType) {
            TokenType.NUMBER -> ASTNode.ImmediateValue((node.value as Number).value)
            TokenType.SYMBOL -> {
                val name = (node.value as Symbol).content
                if (!node.isFunction) {
                    if (checkVariableExistence) {
                        if (!isVariableExists(name)) {
                            throw IllegalArgumentException("Unknown variable $name " + node.value?.at())
                        }
                    }
                    ASTNode.Variable(name)
                } else {
                    val arguments = parseFunctionArguments(node.subNodes ?: listOf())
                    val intFunc = parseInternalFunction(node, name, arguments)
                    if (intFunc != null) return intFunc
                    context.useFunction(name)
                    ASTNode.FunctionCall(name, arguments)
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

    private fun parseInternalFunction(node: Node, name: String, args: List<ASTNode>): ASTNode? {
        return when(name) {
            "neg" -> {
                checkArgumentsCount(node, name, 1)
                val arg = args[0]
                if (arg is ASTNode.ImmediateValue) {
                    ASTNode.ImmediateValue(-arg.value)
                } else {
                    ASTNode.Neg(arg)
                }
            }
            "inv" -> {
                checkArgumentsCount(node, name, 1)
                val arg = args[0]
                if (arg is ASTNode.ImmediateValue) {
                    ASTNode.ImmediateValue(arg.value.inv())
                } else {
                    ASTNode.Inv(arg)
                }
            }
            else -> null
        }
    }

    private fun checkArgumentsCount(node: Node, name: String, count: Int) {
        val argMap = mapOf(
            "neg" to 1,
            "inv" to 1
        )
        if (argMap[name] != count) {
            throw IllegalArgumentException("$count argument(s) expected by $name function " + node.value?.at())
        }
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
            OperatorType.NEQ -> ASTNode.Neq(left, right)
            OperatorType.LT -> ASTNode.Lt(left, right)
            OperatorType.GT -> ASTNode.Gt(left, right)
            OperatorType.GTEQ -> ASTNode.GtEq(left, right)
            OperatorType.LTEQ -> ASTNode.LtEq(left, right)
            OperatorType.ELSE -> ASTNode.Else(left, right)
            OperatorType.WHILE -> ASTNode.While(left, right)
            OperatorType.REPEAT -> ASTNode.Repeat(left, right)
            OperatorType.UNTIL -> ASTNode.Until(left, right)
            OperatorType.SHR -> ASTNode.Shr(left, right)
            OperatorType.SHL -> ASTNode.Shl(left, right)
            OperatorType.ORB -> ASTNode.OrB(left, right)
            OperatorType.ORL -> ASTNode.OrL(left, right)
            OperatorType.ANDB -> ASTNode.AndB(left, right)
            OperatorType.ANDL -> ASTNode.AndL(left, right)
            OperatorType.XOR -> ASTNode.Xor(left, right)
            OperatorType.MOD -> ASTNode.Mod(left, right)
        }
    }

}
