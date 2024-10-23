package org.pingwiner.compiler.optimizer

import org.pingwiner.compiler.parser.ASTNode

class Optimizer(var root: ASTNode) {
    private var optimizationsCount = 0

    fun run(): ASTNode {
        var node = root
        do {
            optimizationsCount = 0
            node = optimizeArithmeticOperationsWithImmediates(node)
        } while (optimizationsCount > 0)
        return node
    }

    private fun optimizeArithmeticOperationsWithImmediates(node: ASTNode) : ASTNode {
        when(node) {
            is ASTNode.BinaryOperation -> {
                if ((node.left is ASTNode.ImmediateValue) && (node.right is ASTNode.ImmediateValue)) {
                    return calculate(node, node.left.value, node.right.value)
                } else {
                    return node.create(
                        if (node.left is ASTNode.ImmediateValue) node.left else optimizeArithmeticOperationsWithImmediates(node.left),
                        if (node.right is ASTNode.ImmediateValue) node.right else optimizeArithmeticOperationsWithImmediates(node.right)
                    )
                }
            }
            is ASTNode.Block -> {
                val newSubNodes = mutableListOf<ASTNode>()
                for (subNode in node.subNodes) {
                    newSubNodes.add(optimizeArithmeticOperationsWithImmediates(subNode))
                }
                return (ASTNode.Block(newSubNodes))
            }
            is ASTNode.FunctionCall -> {
                val newArguments = mutableListOf<ASTNode>()
                for (arg in node.arguments) {
                    newArguments.add(optimizeArithmeticOperationsWithImmediates(arg))
                }
                return ASTNode.FunctionCall(node.name, newArguments)
            }
            is ASTNode.ImmediateValue -> return node
            is ASTNode.Inv -> return ASTNode.Inv(optimizeArithmeticOperationsWithImmediates(node.arg))
            is ASTNode.Neg -> return ASTNode.Neg(optimizeArithmeticOperationsWithImmediates(node.arg))
            is ASTNode.Repeat -> return ASTNode.Repeat(
                optimizeArithmeticOperationsWithImmediates(node.statement),
                optimizeArithmeticOperationsWithImmediates(node.count)
            )
            is ASTNode.Return -> {
                node.value?.let {
                    return(ASTNode.Return(optimizeArithmeticOperationsWithImmediates(it)))
                } ?: return node
            }
            is ASTNode.Until -> return ASTNode.Until(
                optimizeArithmeticOperationsWithImmediates(node.statement),
                optimizeArithmeticOperationsWithImmediates(node.condition)
            )
            is ASTNode.Variable -> {
                node.index?.let { index ->
                    return ASTNode.Variable(node.name, optimizeArithmeticOperationsWithImmediates(index))
                } ?: return node
            }
            is ASTNode.While -> return ASTNode.While(
                optimizeArithmeticOperationsWithImmediates(node.statement),
                optimizeArithmeticOperationsWithImmediates(node.condition)
            )
        }
    }

    private fun calculate(node: ASTNode.BinaryOperation, left: Int, right: Int): ASTNode {
        when (node) {
            is ASTNode.BinaryOperation.If -> return node
            is ASTNode.BinaryOperation.Else -> return node
            is ASTNode.BinaryOperation.Assign -> throw IllegalArgumentException("Illegal operation: $left = $right")
            else -> { optimizationsCount++ }
        }

        return when(node) {
            is ASTNode.BinaryOperation.Plus -> ASTNode.ImmediateValue(left + right)
            is ASTNode.BinaryOperation.And -> ASTNode.ImmediateValue(left and right)
            is ASTNode.BinaryOperation.Divide -> ASTNode.ImmediateValue(left / right)
            is ASTNode.BinaryOperation.Eq -> boolNode(left == right)
            is ASTNode.BinaryOperation.Gt -> boolNode(left > right)
            is ASTNode.BinaryOperation.GtEq -> boolNode(left >= right)
            is ASTNode.BinaryOperation.Lt -> boolNode(left < right)
            is ASTNode.BinaryOperation.LtEq -> boolNode(left <= right)
            is ASTNode.BinaryOperation.Minus -> ASTNode.ImmediateValue(left - right)
            is ASTNode.BinaryOperation.Mod -> ASTNode.ImmediateValue(left % right)
            is ASTNode.BinaryOperation.Multiply -> ASTNode.ImmediateValue(left * right)
            is ASTNode.BinaryOperation.Neq -> boolNode(left != right)
            is ASTNode.BinaryOperation.Or -> ASTNode.ImmediateValue(left or right)
            is ASTNode.BinaryOperation.Shl -> ASTNode.ImmediateValue(left shl right)
            is ASTNode.BinaryOperation.Shr -> ASTNode.ImmediateValue(left shr right)
            is ASTNode.BinaryOperation.Xor -> ASTNode.ImmediateValue(left xor right)
            else -> throw Exception("WTF")
        }
    }

    private fun boolNode(value: Boolean): ASTNode {
        return if (value) ASTNode.ImmediateValue(1) else ASTNode.ImmediateValue(0)
    }

}