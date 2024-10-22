package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.Number
import org.pingwiner.compiler.optimizer.Optimizer

class Function(val name: String, val params: List<String>) {
    private lateinit var context: ParserContext
    val vars = mutableListOf<String>()
    var root: ASTNode? = null

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

    fun parse(tokens: List<Token>, context: ParserContext) {
        this.context = context
        val nodes = convertToNodes(tokens)
        root = parseBlock(nodes)
        root?.let {
            val optimizer = Optimizer(it)
            root = optimizer.run()
        }
    }

    private fun parseBlock(nodes: List<Node>) : ASTNode {
        val statements = mutableListOf<ASTNode>()
        var i = 0
        while (i < nodes.size) {
            val end = searchForEnd(nodes, i)
            val statement = parseStatement(nodes.subList(i, end))
            i = end + 1
            statements.add(statement)
        }
        if (statements.size == 1) return statements[0]
        return ASTNode.Block(statements)
    }

    private fun parseStatement(nodes: List<Node>): ASTNode {
        return parseStatementNodes(wrapBlocks(nodes))
    }

    private fun parseStatementNodes(inputNodes: List<Node>): ASTNode {
        var nodes = wrapFunctionCalls(inputNodes)
        nodes = wrapArrayAccess(nodes)
        nodes = wrapBraces(nodes)

        var level = maxPriorityLevel
        while (level >= 0) {
            val wrapResult = wrapTokensWithPriority(nodes, level)
            nodes = wrapResult.nodes
            if (!wrapResult.wrapped) {
                level--
            }
        }
        return nodesToAst(nodes)
    }

    private fun wrapFunctionCalls(nodes: List<Node>): List<Node>  {
        var i = 0
        val result = mutableListOf<Node>()
        while (i < nodes.size) {
            if (nodes[i].value is SpecialSymbol.LBrace && (i > 0) && nodes[i - 1].value is Symbol) {
                val j = findComplementBraceNode<SpecialSymbol.LBrace, SpecialSymbol.RBrace>(nodes, i)
                if (j == NOT_FOUND) {
                    throw IllegalArgumentException(") not found " + nodes[i].value?.at())
                }
                result.last().subNodes = nodes.subList(i + 1, j)
                result.last().type = NodeType.Function
                i = j + 1
            } else {
                result.add(nodes[i])
                i += 1
            }
        }
        return result
    }

    private fun wrapArrayAccess(nodes: List<Node>): List<Node> {
        var i = 0
        val result = mutableListOf<Node>()
        while (i < nodes.size) {
            if (nodes[i].value is SpecialSymbol.LSquare && (i > 0) && nodes[i - 1].value is Symbol) {
                val j = findComplementBraceNode<SpecialSymbol.LSquare, SpecialSymbol.RSquare>(nodes, i)
                if (j == NOT_FOUND) {
                    throw IllegalArgumentException("] not found " + nodes[i].value?.at())
                }
                result.last().subNodes = nodes.subList(i + 1, j)
                result.last().type = NodeType.ArrayAccess
                i = j + 1
            } else {
                result.add(nodes[i])
                i += 1
            }
        }
        return result
    }

    private fun wrapBraces(nodes: List<Node>): List<Node> {
        val result = mutableListOf<Node>()
        var i = 0
        while(i < nodes.size) {
            val n = nodes[i]
            if (n.value is SpecialSymbol.LBrace) {
                val j = findComplementBraceNode<SpecialSymbol.LBrace, SpecialSymbol.RBrace>(nodes, i)
                if (j == NOT_FOUND) {
                    throw IllegalStateException(") not found " + n.value?.at())
                }
                val subnodes = wrapBraces(nodes.subList(i + 1, j))
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

    private fun wrapBlocks(nodes: List<Node>): List<Node> {
        val result = mutableListOf<Node>()
        var i = 0
        while(i < nodes.size) {
            val n = nodes[i]
            if (n.value is SpecialSymbol.LCurl) {
                val j = findComplementBraceNode<SpecialSymbol.LCurl, SpecialSymbol.RCurl>(nodes, i)
                if (j == NOT_FOUND) {
                    throw IllegalStateException("} not found " + n.value?.at())
                }
                val subnodes = nodes.subList(i + 1, j)
                val combined = Node(subnodes)
                combined.type = NodeType.Block
                result.add(combined)
                i = j + 1
            } else {
                result.add(n)
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
                            if (operatorPriorityMap[n.value!!::class] == priority) {
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
                if (n.type != NodeType.Block) {
                    val wrapResult = wrapTokensWithPriority(n.subNodes!!, priority)
                    wrapped = wrapped or wrapResult.wrapped
                    result.add(Node(wrapResult.nodes))
                } else {
                    result.add(n)
                }
                i++
                continue
            }
            result.add(n)
            i++
        }
        return WrapResult(result, wrapped)
    }

    private fun variableExists(name: String): Boolean {
        if (!context.hasVariable(name)) {
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
            if (nodes[1].value is Operator) {
                val isAssignment = nodes[1].value is Operator.Assign
                val left = nodeToAstNode(nodes[0], !isAssignment)
                if (left is ASTNode.Variable) {
                    val name = left.name
                    if (!variableExists(name)) {
                        vars.add(name)
                    }
                }
                val right = nodeToAstNode(nodes[2])
                return nodeToAstNode(nodes[1], left, right)
            } else {
                throw IllegalArgumentException("Unexpected token " + nodes[1].value?.at())
            }
        } else {
            throw IllegalArgumentException("Syntax error " + nodes[0].value?.at())
        }
    }

    private fun nodeToAstNode(node: Node, checkVariableExistence: Boolean = true): ASTNode {
        return when(node.value) {
            is Number -> ASTNode.ImmediateValue((node.value as Number).value)
            is Symbol -> {
                val name = (node.value as Symbol).content
                if (node.type == NodeType.Function) {
                    //Parse function call
                    val arguments = parseFunctionCallArguments(node.subNodes ?: listOf())
                    val intFunc = parseInternalFunction(node, name, arguments)
                    if (intFunc != null) return intFunc
                    context.useFunction(name)
                    ASTNode.FunctionCall(name, arguments)
                } else {
                    //Parse variable
                    if (checkVariableExistence) {
                        if (!variableExists(name)) {
                            throw IllegalArgumentException("Unknown variable $name " + node.value?.at())
                        }
                    }
                    parseVar(name, node)
                }
            }
            is Keyword -> {
                if (node.value is Keyword.Return) {
                    ASTNode.Return()
                } else {
                    throw IllegalArgumentException("Syntax error " + node.value?.at())
                }
            }
            null -> {
                node.subNodes?.let { subNodes ->
                    if (node.type == NodeType.Block) {
                        parseBlock(subNodes)
                    } else {
                        nodesToAst(subNodes)
                    }
                } ?: throw IllegalArgumentException("Bad token $node")
            }
            else -> { throw IllegalArgumentException("Syntax error " + node.value?.at())}
        }
    }

    private fun parseFunctionCallArguments(nodes: List<Node>): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        if (nodes.isEmpty()) return result
        var i = 0
        while (i < nodes.size) {
            var j = findNextComma(nodes, i)
            if (j == NOT_FOUND) {
                j = nodes.size
            }
            result.add(parseStatementNodes(nodes.subList(i, j)))
            i = j + 1
        }
        return result
    }

    private fun parseVar(name: String, node: Node): ASTNode {
        //Check if array
        return node.subNodes?.let {
            if (!context.hasArray(name)) {
                throw IllegalArgumentException("Unknown array $name " + node.value?.at())
            }
            val index = parseIndex(it)
            if ((index is ASTNode.ImmediateValue) && (index.value >= context.arraySize(name))) {
                throw IllegalArgumentException("Index out of bounds $name[${index.value}] " + node.value?.at())
            }
            ASTNode.Variable(name, index)
        //else create simple var
        } ?: ASTNode.Variable(name)
    }

    private fun parseIndex(nodes: List<Node>): ASTNode {
        return parseStatementNodes(nodes)
    }

    private fun parseInternalFunction(node: Node, name: String, args: List<ASTNode>): ASTNode? {
        return when(name) {
            "neg" -> {
                checkArgumentsCount(node, name, args.size)
                val arg = args[0]
                if (arg is ASTNode.ImmediateValue) {
                    ASTNode.ImmediateValue(-arg.value)
                } else {
                    ASTNode.Neg(arg)
                }
            }
            "inv" -> {
                checkArgumentsCount(node, name, args.size)
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
        return when(op) {
            is Operator.Plus -> ASTNode.BinaryOperation.Plus(left, right)
            is Operator.Minus -> ASTNode.BinaryOperation.Minus(left, right)
            is Operator.Multiply -> ASTNode.BinaryOperation.Multiply(left, right)
            is Operator.Divide -> ASTNode.BinaryOperation.Divide(left, right)
            is Operator.Assign -> {
                if (left is ASTNode.Variable) {
                    ASTNode.BinaryOperation.Assign(left, right)
                } else if (left is ASTNode.Return) {
                    ASTNode.Return(right)
                } else {
                    throw IllegalArgumentException("Illegal assignment " + node.value?.at())
                }
            }
            is Operator.If -> ASTNode.BinaryOperation.If(left, right)
            is Operator.Eq -> ASTNode.BinaryOperation.Eq(left, right)
            is Operator.Neq -> ASTNode.BinaryOperation.Neq(left, right)
            is Operator.Lt -> ASTNode.BinaryOperation.Lt(left, right)
            is Operator.Gt -> ASTNode.BinaryOperation.Gt(left, right)
            is Operator.GtEq -> ASTNode.BinaryOperation.GtEq(left, right)
            is Operator.LtEq -> ASTNode.BinaryOperation.LtEq(left, right)
            is Operator.Else -> ASTNode.BinaryOperation.Else(left, right)
            is Operator.While -> ASTNode.While(left, right)
            is Operator.Repeat -> ASTNode.Repeat(left, right)
            is Operator.Until -> ASTNode.Until(left, right)
            is Operator.Shr -> ASTNode.BinaryOperation.Shr(left, right)
            is Operator.Shl -> ASTNode.BinaryOperation.Shl(left, right)
            is Operator.Or -> ASTNode.BinaryOperation.Or(left, right)
            is Operator.And -> ASTNode.BinaryOperation.And(left, right)
            is Operator.Xor -> ASTNode.BinaryOperation.Xor(left, right)
            is Operator.Mod -> ASTNode.BinaryOperation.Mod(left, right)
        }
    }

}
