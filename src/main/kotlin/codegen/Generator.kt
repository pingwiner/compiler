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
        for (v in program.globalVars.values) {
            if (v.size == 1) {
                v.value?.let {
                    if (it.size == 1) {
                        varValues[v.name] = it[0]
                    }
                }
            }
        }
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

    private fun processNode(node: ASTNode, parent: ASTNode? = null): Operand {
        when (node) {
            is ASTNode.BinaryOperation -> {
                return processBinaryOperation(node, parent)
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

            is ASTNode.Neg -> {
                return processNeg(node)
            }

            is ASTNode.Inv -> {
                return processInv(node)
            }

            is ASTNode.Repeat -> {
                TODO("Not implemented")
            }

            is ASTNode.Variable -> {
                if (node.index != null) {
                    val indexOperand = processNode(node.index)
                    val baseOperand = makeOperand(node.name)
                    val reg = nextRegister()
                    val result = Operand(reg, OperandType.Register)
                    operations.add(Operation.Load(result, baseOperand, indexOperand))
                    return result
                }
                if (varValues.contains(node.name)) {
                    val v: Int = varValues[node.name]!!
                    return Operand(v.toString(), OperandType.ImmediateValue, v)
                } else if (varRefs.contains(node.name)) {
                    val ref = varRefs[node.name]!!
                    return Operand(ref, OperandType.Register)
                } else {
                    return makeOperand(node.name)
                }
            }

            is ASTNode.Block -> {
                var result: Operand? = null
                for (subNode in node.subNodes) {
                    result = processNode(subNode)
                }
                return result ?: Default()
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
        for (ref in generator.varValues.keys) {
            varValues.remove(ref)
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

    private fun nextRegister(): String {
        return "R${regCount++}"
    }

    private fun jumpIfNot(operand: Operand, label: String) : Operation.IfNot {
        return Operation.IfNot(operand, Operand(label, OperandType.Label))
    }

    private fun jump(label: String) : Operation.Goto {
        return Operation.Goto(Operand(label, OperandType.Label))
    }

    private fun processBinaryOperation(node: ASTNode.BinaryOperation, parent: ASTNode? = null): Operand {
        if (node is ASTNode.BinaryOperation.Assign) {
            return processAssign(node)
        }

        if (node is ASTNode.BinaryOperation.If) {
            return processIf(node, parent)
        }

        //Arithmetic and logic operations

        val reg = nextRegister()

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

    private fun makeBranch(node: ASTNode, generator: Generator, needReturnValue: Boolean) : Operand {
        when (node) {
            is ASTNode.ImmediateValue -> {
                if (needReturnValue) {
                    operations.add(Operation.SetResult(Operand("", OperandType.ImmediateValue, node.value)))
                }
            }

            is ASTNode.Variable -> {
                operations.addAll(generator.operations)
                if (needReturnValue) {
                    if (generator.operations.size > 0) {
                        operations.add(makeResult(operations.last().result))
                    } else {
                        operations.add(Operation.SetResult(makeOperand(node.name)))
                    }
                }
            }

            else -> {
                operations.addAll(generator.operations)
                if (needReturnValue) {
                    if (generator.operations.size > 0) {
                        operations.add(makeResult(operations.last().result))
                    } else {
                        operations.add(Operation.SetResult(Default()))
                    }
                }
            }
        }
        return operations.last().result
    }

    private fun makeResult(op: Operand) : Operation.SetResult {
        if (op.type == OperandType.ImmediateValue) {
            return Operation.SetResult(op)
        }
        if (op.type == OperandType.LocalVariable) {
            op.value?.let {
                return Operation.SetResult(
                    Operand(op.name, OperandType.ImmediateValue, it)
                )
            }
        }
        if (op.type == OperandType.Register) {
            if (varValues.containsKey(op.name)) {
                return Operation.SetResult(
                    Operand(op.name, OperandType.ImmediateValue, varValues[op.name])
                )
            }
        }
        return Operation.SetResult(op)
    }

    private fun processIf(node: ASTNode.BinaryOperation.If, parent: ASTNode? = null): Operand {
        val condition = Generator(program)
        condition.generateFrom(node.left)
        val opCount = operations.size
        operations.addAll(condition.operations)
        val label = nextLabel()
        val label2 = nextLabel()
        operations.add(jumpIfNot(operations.last().result, label))
        clearRefs(condition)

        lateinit var result1 : Operand
        lateinit var result2 : Operand
        val needReturnValue = parent is ASTNode.BinaryOperation.Assign
        var hasUsefulOperations = false
        if (node.right is ASTNode.BinaryOperation.Else) {
            val thenBranch = Generator(program)
            thenBranch.generateFrom(node.right.left)
            result1 = makeBranch(node.right.left, thenBranch, needReturnValue)
            operations.add(jump(label2))
            operations.add(Operation.Label(label))

            val elseBranch = Generator(program)
            elseBranch.generateFrom(node.right.right)
            result2 = makeBranch(node.right.right, elseBranch, needReturnValue)
            operations.add(Operation.Label(label2))

            if ((thenBranch.operations.size > 0) || (elseBranch.operations.size > 0) || needReturnValue) {
                hasUsefulOperations = true
                clearRefs(thenBranch)
                clearRefs(elseBranch)
            }
        } else {
            val thenBranch = Generator(program)
            thenBranch.generateFrom(node.right)
            result1 = makeBranch(node.right, thenBranch, needReturnValue)
            operations.add(Operation.Label(label))
            result2 = Default()
            if (thenBranch.operations.size > 0 || needReturnValue) {
                hasUsefulOperations = true
                clearRefs(thenBranch)
            }
        }
        if (!hasUsefulOperations) {
            operations = operations.dropLast(operations.size - opCount).toMutableList()
            return Default()
        }
        if (result1.theSameAs(result2)) {
            return result1
        }
        return Phi(result1, result2)
    }

    private fun processAssign(node: ASTNode.BinaryOperation.Assign): Operand {
        if (node.left is ASTNode.Variable) {
            if (node.left.index != null) {
                return processStore(node.left, node.right)
            }
            if (node.right is ASTNode.ImmediateValue) {
                varValues[node.left.name] = node.right.value
                varRefs.remove(node.left.name)
                val result = makeOperand(node.left.name, node.right.value)
                val operand = Operand(node.right.value.toString(), OperandType.ImmediateValue, node.right.value)
                operations.add(Operation.Assignment(result, operand))
                return result
            } else if (node.right is ASTNode.Variable && node.right.index == null) {
                varRefs.remove(node.left.name)
                if (varValues.contains(node.right.name)) {
                    varValues[node.left.name] = varValues[node.right.name]!!
                    val result = makeOperand(node.left.name)
                    val operand = Operand(node.right.name, OperandType.ImmediateValue, varValues[node.right.name]!!)
                    operations.add(Operation.Assignment(result, operand))
                    return result
                } else {
                    varValues.remove(node.left.name)
                    val result = makeOperand(node.left.name)
                    val operand = if (varRefs.contains(node.right.name)) {
                        Operand(varRefs[node.right.name]!!, OperandType.Register)
                    } else {
                        makeOperand(node.right.name)
                    }
                    operations.add(Operation.Assignment(result, operand))
                    return result
                }
            } else {
                varValues.remove(node.left.name)
                val result = makeOperand(node.left.name)
                val operand = processNode(node.right, node)
                operations.add(Operation.Assignment(result, operand))
                if (result.type == OperandType.LocalVariable) {
                    if (operand.value != null) {
                        varValues[node.left.name] = operand.value
                        varRefs.remove(node.left.name)
                    } else {
                        if (operand.type == OperandType.Register) {
                            varRefs[node.left.name] = operand.name
                        }
                    }
                }
                return result
            }
        } else {
            throw IllegalStateException("WTF")
        }
    }

    private fun processStore(left: ASTNode.Variable, right: ASTNode): Operand {
        val baseOperand = makeOperand(left.name)
        val indexOperand = processNode(left.index!!)
        val resultOperand = processNode(right)
        operations.add(Operation.Store(resultOperand, baseOperand, indexOperand))
        return resultOperand
    }

    private fun processNeg(node: ASTNode.Neg): Operand {
        val n = processNode(node.arg)
        val reg = nextRegister()
        if (n.type == OperandType.ImmediateValue) {
            val newVal = -(n.value ?: 0)
            return Operand(newVal.toString(), OperandType.ImmediateValue, newVal)
        }

        val op = Operation.Neg(Operand(reg, OperandType.Register), n)
        operations.add(op)
        return operations.last().result
    }

    private fun processInv(node: ASTNode.Inv): Operand {
        val n = processNode(node.arg)
        val reg = nextRegister()
        if (n.type == OperandType.ImmediateValue) {
            val newVal = n.value?.inv() ?: 0
            return Operand(newVal.toString(), OperandType.ImmediateValue, newVal)
        }

        val op = Operation.Inv(Operand(reg, OperandType.Register), n)
        operations.add(op)
        return operations.last().result
    }

    private fun makeOperand(name: String, value: Int? = null): Operand {
        if (program.hasVariable(name)) return Operand(name, OperandType.GlobalVariable, value)
        return Operand(name, OperandType.LocalVariable, value)
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
                        if (op.operand is Phi) {
                            if (op.operand.op1.type != OperandType.ImmediateValue) {
                                usedVars.add(op.operand.op1.name)
                            }
                            if (op.operand.op2.type != OperandType.ImmediateValue) {
                                usedVars.add(op.operand.op2.name)
                            }
                        } else {
                            usedVars.add(op.operand.name)
                        }
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
                is Operation.SetResult -> {
                    usedVars.add(op.result.name)
                }
                is Operation.Neg -> {
                    if (op.operand.type != OperandType.ImmediateValue) {
                        usedVars.add(op.operand.name)
                    }
                }
                is Operation.Inv -> {
                    if (op.operand.type != OperandType.ImmediateValue) {
                        usedVars.add(op.operand.name)
                    }
                }
                is Operation.Load -> {
                    if (op.index.type != OperandType.ImmediateValue) {
                        usedVars.add(op.index.name)
                    }
                }
                is Operation.Store -> {
                    if (op.index.type != OperandType.ImmediateValue) {
                        usedVars.add(op.index.name)
                    }
                }
            }
        }
        val newOperations = mutableListOf<Operation>()
        for (op in operations) {
            if (usedVars.contains(op.result.name) || op is Operation.IfNot || op is Operation.Label
                || op is Operation.Goto || op is Operation.Load || op is Operation.Store) {
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