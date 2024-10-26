package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.ASTNode

class Generator {
    val varValues = mutableMapOf<String, Int>()

    companion object {
        var regCount = 0
    }

    val operations = mutableListOf<Operation>()

    fun processNode(node: ASTNode): Operand {
        when(node) {
            is ASTNode.BinaryOperation -> {
                return processBinaryOperation(node)
            }
            is ASTNode.ImmediateValue -> {
                return Operand(node.value.toString(), OperandType.ImmediateValue, node.value)
            }
            is ASTNode.Variable -> {
                if (varValues.contains(node.name)) {
                    val v: Int = varValues[node.name]!!
                    return Operand(v.toString(), OperandType.ImmediateValue, v)
                } else {
                    return Operand(node.name, OperandType.LocalVariable)
                }
            }
            is ASTNode.Block -> {
                var result: Operand? = null
                for (subNode in node.subNodes) {
                    result = processNode(subNode)
                }
                return result!!
            }
            else -> TODO()
        }
        throw IllegalStateException("Not implemented")
    }

    private fun processBinaryOperation(node: ASTNode.BinaryOperation): Operand {
        if (node is ASTNode.BinaryOperation.Assign) {
            if (node.left is ASTNode.Variable) {
                if (node.right is ASTNode.ImmediateValue) {
                    varValues[node.left.name] = node.right.value
                    val result = Operand(node.left.name, OperandType.LocalVariable, node.right.value)
                    val operand = Operand(node.right.value.toString(), OperandType.ImmediateValue, node.right.value)
                    operations.add(Operation.Assignment(result, operand))
                    return result
                } else if (node.right is ASTNode.Variable) {
                    if (varValues.contains(node.right.name)) {
                        varValues[node.left.name] = varValues[node.right.name]!!
                        val result = Operand(node.left.name, OperandType.LocalVariable)
                        val operand = Operand(node.right.name, OperandType.ImmediateValue, varValues[node.right.name]!!)
                        operations.add(Operation.Assignment(result, operand))
                        return result
                    } else {
                        varValues.remove(node.left.name)
                        val result = Operand(node.left.name, OperandType.LocalVariable)
                        val operand = Operand(node.right.name, OperandType.LocalVariable)
                        operations.add(Operation.Assignment(result, operand))
                        return result
                    }
                } else {
                    throw IllegalStateException("WTF")
                }
            } else {
                throw IllegalStateException("WTF")
            }
        }
        val reg = "R${regCount++}"

        val leftVal = processNode(node.left)
        val rightVal = processNode(node.right)

        if (leftVal.type == OperandType.ImmediateValue && rightVal.type == OperandType.ImmediateValue) {
            val v = calculate(node, leftVal.value!!, rightVal.value!!)
            val result = Operand(reg, OperandType.ImmediateValue, v)
            operations.add(Operation.Assignment(result, Operand(v.toString(), OperandType.ImmediateValue, v)))
            varValues[reg] = v
            return result
        }

        val operator = Operator.from(node)

        val isZero = when (operator) {
            Operator.MULTIPLY -> leftVal.isZero() || rightVal.isZero()
            Operator.DIVIDE ->  leftVal.isZero()
            Operator.AND -> leftVal.isZero() || rightVal.isZero()
            else -> false
        }

        if (isZero) {
            val result = Operand(reg, OperandType.ImmediateValue, 0)
            operations.add(Operation.Assignment(result, Operand("0", OperandType.ImmediateValue, 0)))
            return result
        }

        val result = Operand(reg, OperandType.Register)
        operations.add(Operation.BinaryOperation(result, leftVal, rightVal, operator))
        return result
    }


    private fun calculate(node: ASTNode.BinaryOperation, left: Int, right: Int): Int {
        if (node is ASTNode.BinaryOperation.If || node is ASTNode.BinaryOperation.Else || node is ASTNode.BinaryOperation.Assign) {
            throw IllegalStateException("Not implemented")
        }

        return when(node) {
            is ASTNode.BinaryOperation.Plus -> left + right
            is ASTNode.BinaryOperation.And -> left and right
            is ASTNode.BinaryOperation.Divide -> left / right
            is ASTNode.BinaryOperation.Eq -> boolToInt(left == right)
            is ASTNode.BinaryOperation.Gt -> boolToInt(left > right)
            is ASTNode.BinaryOperation.GtEq -> boolToInt(left >= right)
            is ASTNode.BinaryOperation.Lt -> boolToInt(left < right)
            is ASTNode.BinaryOperation.LtEq -> boolToInt(left <= right)
            is ASTNode.BinaryOperation.Minus -> left - right
            is ASTNode.BinaryOperation.Mod -> left % right
            is ASTNode.BinaryOperation.Multiply -> left * right
            is ASTNode.BinaryOperation.Neq -> boolToInt(left != right)
            is ASTNode.BinaryOperation.Or -> left or right
            is ASTNode.BinaryOperation.Shl -> left shl right
            is ASTNode.BinaryOperation.Shr -> left shr right
            is ASTNode.BinaryOperation.Xor -> left xor right
            else -> throw Exception("WTF")
        }
    }

    private fun boolToInt(b: Boolean) = if (b) 1 else 0

    fun printOperations() {
        for (op in operations) {
            println(op.toString())
        }
    }
}