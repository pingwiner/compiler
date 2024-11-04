package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.ASTNode
import org.pingwiner.compiler.parser.Program
import org.pingwiner.compiler.parser.Function

class Generator(val program: Program) {
    private val varValues = mutableMapOf<String, Int>()
    private val varRefs = mutableMapOf<String, String>()
    companion object {
        var regCount = 0
        var labelCount = 0
    }

    val irFunctions = mutableMapOf<String, IRFunction>()

    private var operations = mutableListOf<Operation>()
    private lateinit var currentFunction: Function
    private var usedVars = mutableSetOf<String>()

    fun generate() {
        for (function in program.functions) {
            currentFunction = function
            operations = mutableListOf()
            operations.add(Operation.Label(currentFunction.name))
            usedVars = mutableSetOf()
            regCount = 0
            processNode(currentFunction.root!!)
            removeUselessOperations()
            val usedLocalVars = mutableListOf<String>()
            for (v in currentFunction.vars) {
                if (usedVars.contains(v)) usedLocalVars.add(v)
            }

            irFunctions[currentFunction.name] = IRFunction(
                currentFunction.name,
                currentFunction.params,
                usedLocalVars,
                operations
            )
            printOperations()
        }
    }

    private fun generateFrom(node: ASTNode) {
        processNode(node)
    }

    private fun processNode(node: ASTNode): Operand {
        when (node) {
            is ASTNode.BinaryOperation -> {
                return processBinaryOperation(node)
            }

            is ASTNode.While -> {
                return processWhileOperation(node)
            }

            is ASTNode.Until -> {
                return processUntilOperation(node)
            }

            is ASTNode.ImmediateValue -> {
                return Operand(node.value.toString(), OperandType.ImmediateValue, node.value)
            }

            is ASTNode.FunctionCall -> {
                return processFunctionCall(node)
            }

            is ASTNode.Variable -> {
                if (varValues.contains(node.name)) {
                    val v: Int = varValues[node.name]!!
                    return Operand(v.toString(), OperandType.ImmediateValue, v)
                } else if (varRefs.contains(node.name)) {
                    val ref = varRefs[node.name]!!
                    return Operand(ref, OperandType.Register)
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
            is ASTNode.Return -> {
                return processReturn(node)
            }
            else -> TODO()
        }
    }

    private fun processFunctionCall(node: ASTNode.FunctionCall): Operand {
        val callArgs = mutableListOf<Operand>()
        for (arg in node.arguments) {
            val callArg = processNode(arg)
            callArgs.add(callArg)
        }
        operations.add(Operation.Call(Operand(node.name, OperandType.Label), callArgs))
        return operations.last().result
    }

    private fun processUntilOperation(node: ASTNode.Until): Operand {
        val labelStart = nextLabel()
        operations.add(Operation.Label(labelStart))
        val genStatement = Generator(program)
        genStatement.generateFrom(node.statement)
        operations.addAll(genStatement.operations)
        val result = operations.last().result
        val genCondition = Generator(program)
        genCondition.generateFrom(node.condition)
        operations.addAll(genCondition.operations)
        operations.add(jumpIfNot(operations.last().result, labelStart))
        clearRefs(genStatement)
        clearRefs(genCondition)
        return result
    }

    private fun clearRefs(generator: Generator) {
        for (ref in generator.varRefs.keys) {
            varRefs.remove(ref)
        }
    }

    private fun processWhileOperation(node: ASTNode.While): Operand {
        val genCondition = Generator(program)
        genCondition.generateFrom(node.condition)
        val labelStart = nextLabel()
        val labelEnd = nextLabel()
        operations.add(Operation.Label(labelStart))
        operations.addAll(genCondition.operations)
        clearRefs(genCondition)
        operations.add(jumpIfNot(operations.last().result, labelEnd))

        val genStatement = Generator(program)
        genStatement.generateFrom(node.statement)
        operations.addAll(genStatement.operations)
        clearRefs(genStatement)

        val result = operations.last().result
        operations.add(jump(labelStart))
        operations.add(Operation.Label(labelEnd))
        return result
    }

    private fun nextLabel() : String {
        return "label${labelCount++}"
    }

    private fun jumpIfNot(operand: Operand, label: String) : Operation.IfNot {
        return Operation.IfNot(operand, Operand(label, OperandType.Label))
    }

    private fun jump(label: String) : Operation.Goto {
        return Operation.Goto(Operand(label, OperandType.Label))
    }

    private fun processBinaryOperation(node: ASTNode.BinaryOperation): Operand {
        if (node is ASTNode.BinaryOperation.Assign) {
            if (node.left is ASTNode.Variable) {
                if (node.right is ASTNode.ImmediateValue) {
                    varValues[node.left.name] = node.right.value
                    varRefs.remove(node.left.name)
                    val result = Operand(node.left.name, OperandType.LocalVariable, node.right.value)
                    val operand = Operand(node.right.value.toString(), OperandType.ImmediateValue, node.right.value)
                    operations.add(Operation.Assignment(result, operand))
                    return result
                } else if (node.right is ASTNode.Variable) {
                    varRefs.remove(node.left.name)
                    if (varValues.contains(node.right.name)) {
                        varValues[node.left.name] = varValues[node.right.name]!!
                        val result = Operand(node.left.name, OperandType.LocalVariable)
                        val operand = Operand(node.right.name, OperandType.ImmediateValue, varValues[node.right.name]!!)
                        operations.add(Operation.Assignment(result, operand))
                        return result
                    } else {
                        varValues.remove(node.left.name)
                        val result = Operand(node.left.name, OperandType.LocalVariable)
                        val operand = if (varRefs.contains(node.right.name)) {
                            Operand(varRefs[node.right.name]!!, OperandType.Register)
                        } else {
                            Operand(node.right.name, OperandType.LocalVariable)
                        }
                        operations.add(Operation.Assignment(result, operand))
                        return result
                    }
                } else {
                    val result = Operand(node.left.name, OperandType.LocalVariable)
                    val operand = processNode(node.right)
                    operations.add(Operation.Assignment(result, operand))
                    if (operand.value != null) {
                        varValues[node.left.name] = operand.value
                        varRefs.remove(node.left.name)
                    } else {
                        if (operand.type == OperandType.Register) {
                            varRefs[node.left.name] = operand.name
                        }
                    }
                    return result
                }
            } else {
                throw IllegalStateException("WTF")
            }
        }

        if (node is ASTNode.BinaryOperation.If) {
            val genLeft = Generator(program)
            genLeft.generateFrom(node.left)
            operations.addAll(genLeft.operations)
            val label = nextLabel()
            operations.add(jumpIfNot(operations.last().result, label))
            clearRefs(genLeft)
            if (node.right is ASTNode.BinaryOperation.Else) {
                val genThen = Generator(program)
                genThen.generateFrom(node.right.left)
                val genElse = Generator(program)
                genElse.generateFrom(node.right.right)
                operations.addAll(genThen.operations)
                operations.add(Operation.Label(label))
                operations.addAll(genElse.operations)
                clearRefs(genThen)
                clearRefs(genElse)
            } else {
                val genRight = Generator(program)
                genRight.generateFrom(node.right)
                operations.add(Operation.Label(label))
                clearRefs(genRight)
            }

            return operations.last().result
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
        println()
    }

    private fun removeUselessOperations() {
        if (operations.isEmpty()) return
        usedVars = mutableSetOf<String>()
        for (op in operations) {
            if (program.hasVariable(op.result.name)) {
                usedVars.add(op.result.name)
            }
            when(op) {
                is Operation.Assignment -> {
                    if (op.operand.type != OperandType.ImmediateValue) {
                        usedVars.add(op.operand.name)
                    }
                }
                is Operation.BinaryOperation -> {
                    if (op.operand1.type != OperandType.ImmediateValue) {
                        usedVars.add(op.operand1.name)
                    }
                    if (op.operand2.type != OperandType.ImmediateValue) {
                        usedVars.add(op.operand2.name)
                    }
                }
                is Operation.Return -> {
                    if (op.result.name.isNotBlank()) {
                        usedVars.add(op.result.name)
                    }
                }
                is Operation.IfNot -> {
                    if (op.condition.name.isNotBlank()) {
                        usedVars.add(op.condition.name)
                    }
                }
                is Operation.Label -> {}
                is Operation.Goto -> {}
                is Operation.Call -> {
                    for (arg in op.args) {
                        usedVars.add(arg.name)
                    }
                }
                is Operation.Ret -> {
                    usedVars.add(op.result.name)
                }
            }
        }
        val newOperations = mutableListOf<Operation>()
        for (op in operations) {
            if (usedVars.contains(op.result.name) || op is Operation.IfNot || op is Operation.Label || op is Operation.Goto) {
                newOperations.add(op)
            }
        }
        if (newOperations.last() != operations.last()) {
            newOperations.add(operations.last())
        }
        operations.clear()
        operations.addAll(newOperations)
    }

    private fun processReturn(node: ASTNode.Return) : Operand {
        node.value?.let {
            val result = processNode(it)
            operations.add(Operation.Return(result))
            return result
        }
        val result = Operand("", OperandType.ImmediateValue, 0)
        operations.add(Operation.Return(result))
        return result
    }
}