package org.pingwiner.compiler.optimizer

import org.pingwiner.compiler.parser.ASTNode

class Optimizer(var root: ASTNode) {

    fun run(): ASTNode {
        return optimizeArithmeticOperationsWithImmediates(root)
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
            is ASTNode.Block -> TODO()
            is ASTNode.FunctionCall -> TODO()
            is ASTNode.ImmediateValue -> TODO()
            is ASTNode.Inv -> TODO()
            is ASTNode.Neg -> TODO()
            is ASTNode.Repeat -> TODO()
            is ASTNode.Return -> TODO()
            is ASTNode.Until -> TODO()
            is ASTNode.Variable -> TODO()
            is ASTNode.While -> TODO()
        }
    }

    private fun calculate(node: ASTNode.BinaryOperation, left: Int, right: Int): ASTNode {
        return when(node) {
            is ASTNode.BinaryOperation.Plus -> ASTNode.ImmediateValue(left + right)
            is ASTNode.BinaryOperation.And -> ASTNode.ImmediateValue(left and right)
            is ASTNode.BinaryOperation.Assign -> throw IllegalArgumentException("Illegal operation: $left = $right")
            is ASTNode.BinaryOperation.Divide -> ASTNode.ImmediateValue(left / right)
            is ASTNode.BinaryOperation.Else -> node
            is ASTNode.BinaryOperation.Eq -> boolNode(left == right)
            is ASTNode.BinaryOperation.Gt -> boolNode(left > right)
            is ASTNode.BinaryOperation.GtEq -> boolNode(left >= right)
            is ASTNode.BinaryOperation.If -> node
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
        }
    }

    private fun boolNode(value: Boolean): ASTNode {
        return if (value) ASTNode.ImmediateValue(1) else ASTNode.ImmediateValue(0)
    }

}