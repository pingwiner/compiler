package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.codegen.pdp11.PDP11Generator
import org.pingwiner.compiler.parser.ASTNode
import org.pingwiner.compiler.parser.Program
import org.pingwiner.compiler.parser.Function

class VarValues {
    private val varValueMap = mutableMapOf<String, Int>()
    private val _dirty = mutableSetOf<String>()

    operator fun get(key: String): Int? {
        return varValueMap[key]
    }

    operator fun set(key: String, value: Int) {
        _dirty.add(key)
        varValueMap[key] = value
    }

    val dirty: Set<String> = _dirty

    fun clear() {
        varValueMap.clear()
        _dirty.clear()
    }

    fun remove(key: String) {
        _dirty.add(key)
        varValueMap.remove(key)
    }

    fun contains(key: String) = varValueMap.contains(key)
}

class IrGenerator(val program: Program) {
    private val varRefs = mutableMapOf<String, String>()
    private val varValues = VarValues()

    companion object {
        var regCount = 0
        var labelCount = 0
    }

    val irFunctions = mutableMapOf<String, IRFunction>()

    private var operations = mutableListOf<Operation>()
    private lateinit var currentFunction: Function
    private var usedVars = mutableSetOf<String>()

    fun generate(): String {
        for (v in program.globalVars.values) {
            if (v.size == 1) {
                v.value?.let {
                    if (it.size == 1) {
                        varValues[v.name] = it[0]
                    }
                }
            }
        }
        val gen = PDP11Generator(program)
        for (function in program.functions) {
            varValues.clear()
            currentFunction = function
            operations = mutableListOf()
            operations.add(Operation.Label(currentFunction.name))
            usedVars = mutableSetOf()
            regCount = 0
            processNode(currentFunction.root!!)
            removeTemporaryVariables()
            removeUselessOperations()
            removeUselessIf()
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
            //printOperations(operations)
            gen.addFunction(currentFunction.name, operations)
        }
        return gen.generateAssemblyCode()
    }

    private fun generateFrom(node: ASTNode) {
        processNode(node)
    }

    private fun processNode(node: ASTNode, parent: ASTNode? = null): Operand =
        when (node) {
            is ASTNode.BinaryOperation -> processBinaryOperation(node, parent)
            is ASTNode.While -> processWhileOperation(node)
            is ASTNode.Until -> processUntilOperation(node)
            is ASTNode.ImmediateValue -> Operand(node.value.toString(), OperandType.ImmediateValue, node.value)
            is ASTNode.FunctionCall -> processFunctionCall(node)
            is ASTNode.Neg -> processNeg(node)
            is ASTNode.Inv -> processInv(node)
            is ASTNode.Repeat -> processRepeat(node)
            is ASTNode.Variable -> processVariable(node)
            is ASTNode.Block -> processBlock(node)
            is ASTNode.Return -> processReturn(node)
        }

    private fun processRepeat(node: ASTNode.Repeat): Operand {
        TODO("Not implemented")
    }

    private fun processBlock(node: ASTNode.Block): Operand {
        var result: Operand? = null
        for (subNode in node.subNodes) {
            result = processNode(subNode)
        }
        return result ?: Default()
    }

    private fun processVariable(node: ASTNode.Variable): Operand {
        // Array access
        if (node.index != null) {
            val indexOperand = processNode(node.index)
            val baseOperand = makeOperand(node.name)
            val reg = nextRegister()
            val result = Operand(reg, OperandType.Register)
            operations.add(Operation.Load(result, baseOperand, indexOperand))
            return result
        }

        // Constant propagation
        varValues[node.name]?.let {
            return Operand(it.toString(), OperandType.ImmediateValue, it)
        }

        // SSA register
        varRefs[node.name]?.let {
            return Operand(it, OperandType.Register)
        }

        // Local or global variable operand
        return makeOperand(node.name)
    }

    private fun processFunctionCall(node: ASTNode.FunctionCall): Operand {
        val callArgs = mutableListOf<Operand>()
        for (arg in node.arguments) {
            val callArg = processNode(arg)
            callArgs.add(callArg)
        }
        val reg = nextRegister()
        val result = Operand(reg, OperandType.Register)
        operations.add(Operation.Call(result, Operand(node.name, OperandType.Label), callArgs))
        return operations.last().result
    }

    private fun processUntilOperation(node: ASTNode.Until): Operand {
        val labelStart = nextLabel()
        operations.add(Operation.Label(labelStart))
        val genStatement = IrGenerator(program)
        genStatement.generateFrom(node.statement)
        operations.addAll(genStatement.operations)
        val result = operations.last().result
        val genCondition = IrGenerator(program)
        genCondition.generateFrom(node.condition)
        operations.addAll(genCondition.operations)
        operations.add(jumpIfNot(operations.last().result, labelStart))
        clearRefs(genStatement)
        clearRefs(genCondition)
        return result
    }

    private fun clearRefs(irGenerator: IrGenerator) {
        for (ref in irGenerator.varRefs.keys) {
            varRefs.remove(ref)
        }
        for (ref in irGenerator.varValues.dirty) {
            varValues.remove(ref)
        }
    }

    private fun processWhileOperation(node: ASTNode.While): Operand {
        val genCondition = IrGenerator(program)
        genCondition.generateFrom(node.condition)
        val labelStart = nextLabel()
        val labelEnd = nextLabel()
        operations.add(Operation.Label(labelStart))
        operations.addAll(genCondition.operations)
        clearRefs(genCondition)
        operations.add(jumpIfNot(operations.last().result, labelEnd))

        val genStatement = IrGenerator(program)
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
        return "%${regCount++}"
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

        // SSA register for result
        val reg = nextRegister()

        val leftVal = processNode(node.left)
        val rightVal = processNode(node.right)

        // Compile-time calculations
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

        val useless = when (operator) {
            Operator.MULTIPLY -> leftVal.isOne() || rightVal.isOne()
            Operator.DIVIDE -> rightVal.isOne()
            Operator.PLUS -> leftVal.isZero() || rightVal.isZero()
            Operator.MINUS -> rightVal.isZero()
            Operator.OR -> leftVal.isZero() || rightVal.isZero()
            Operator.SHL -> rightVal.isZero()
            Operator.SHR -> rightVal.isZero()
            else -> false
        }

        val result = Operand(reg, OperandType.Register)
        operations.add(Operation.BinaryOperation(result, leftVal, rightVal, operator))
        return result
    }

    // If - Else branch generation
    private fun makeBranch(node: ASTNode, irGenerator: IrGenerator, needReturnValue: Boolean) : Operand {
        when (node) {
            is ASTNode.ImmediateValue -> {
                if (needReturnValue) {
                    operations.add(Operation.SetResult(Operand("", OperandType.ImmediateValue, node.value)))
                }
            }

            is ASTNode.Variable -> {
                operations.addAll(irGenerator.operations)
                if (needReturnValue) {
                    if (irGenerator.operations.size > 0) {
                        operations.add(makeResult(operations.last().result))
                    } else {
                        operations.add(Operation.SetResult(makeOperand(node.name)))
                    }
                }
            }

            else -> {
                operations.addAll(irGenerator.operations)
                if (needReturnValue) {
                    if (irGenerator.operations.size > 0) {
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
            if (varValues.contains(op.name)) {
                return Operation.SetResult(
                    Operand(op.name, OperandType.ImmediateValue, varValues[op.name])
                )
            }
        }
        return Operation.SetResult(op)
    }

    private fun processIf(node: ASTNode.BinaryOperation.If, parent: ASTNode? = null): Operand {
        val condition = IrGenerator(program)
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
            val thenBranch = IrGenerator(program)
            thenBranch.generateFrom(node.right.left)
            result1 = makeBranch(node.right.left, thenBranch, needReturnValue)
            var jmpGenerated = false
            if (operations.last() !is Operation.Return) {
                operations.add(jump(label2))
                jmpGenerated = true
            }
            operations.add(Operation.Label(label))

            val elseBranch = IrGenerator(program)
            elseBranch.generateFrom(node.right.right)
            result2 = makeBranch(node.right.right, elseBranch, needReturnValue)
            if (jmpGenerated) {
                operations.add(Operation.Label(label2))
            }

            if ((thenBranch.operations.size > 0) || (elseBranch.operations.size > 0) || needReturnValue) {
                hasUsefulOperations = true
                clearRefs(thenBranch)
                clearRefs(elseBranch)
            }
        } else {
            val thenBranch = IrGenerator(program)
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

    fun printOperations(ops: List<Operation>) {
        for (op in ops) {
            println(op.toString())
        }
        println()
    }

    private fun removeTemporaryVariables() {
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

    private fun removeUselessOperations() {
        var removed = false
        do {
            removed = false
            val toKeep = mutableListOf<Boolean>()
            for (op in operations) {
                when (op.result.type) {
                    OperandType.ImmediateValue,
                    OperandType.Label,
                    OperandType.GlobalVariable,
                    OperandType.Phi -> {
                        toKeep.add(true)
                    }

                    OperandType.Register,
                    OperandType.LocalVariable -> {
                        val name = op.result.name
                        if (isOperandUsed(name)) {
                            toKeep.add(true)
                        } else {
                            toKeep.add(false)
                            removed = true
                        }
                    }
                }
            }
            val newOperations = mutableListOf<Operation>()
            var i = 0
            for (op in operations) {
                if (toKeep[i]) {
                    newOperations.add(op)
                }
                i += 1
            }
            operations.clear()
            operations.addAll(newOperations)
        } while (removed)
    }

    private fun isOperandUsed(name: String): Boolean {
        for (op in operations) {
            when (op) {
                is Operation.BinaryOperation -> {
                    if (op.operand1.name == name || op.operand2.name == name) {
                        return true
                    }
                }
                is Operation.Assignment -> {
                    if (op.operand.name == name) {
                        return true
                    }
                }
                is Operation.Call -> {
                    for (arg in op.args) {
                        if (arg.name == name) {
                            return true
                        }
                    }
                }
                is Operation.Goto -> {}
                is Operation.IfNot -> {
                    if (op.condition.name == name) {
                        return true
                    }
                }
                is Operation.Inv -> {
                    if (op.operand.name == name) {
                        return true
                    }
                }
                is Operation.Label -> {}
                is Operation.Load -> {
                    if (op.index.name == name) {
                        return true
                    }
                }
                is Operation.Neg -> {
                    if (op.operand.name == name) {
                        return true
                    }
                }
                is Operation.Return -> {
                    if (op.result.name == name) {
                        return true
                    }
                }
                is Operation.SetResult -> {
                    if (op.result.name == name) {
                        return true
                    }
                }
                is Operation.Store -> {
                    if (op.index.name == name) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun removeUselessIf() {
        var prevOp: Operation? = null
        var prevOp2: Operation? = null
        var prevOp3: Operation? = null
        var i = 0
        val toRemove = mutableListOf<Int>()
        for (op in operations) {
            if (op is Operation.Label) {
                if (prevOp is Operation.IfNot) {
                    if (op.result.name == prevOp.result.name) {
                        toRemove.add(i - 1)
                        toRemove.add(i)
                    }
                } else if ((prevOp is Operation.Label) && (prevOp2 is Operation.Goto) && (prevOp3 is Operation.IfNot)) {
                    if ((op.result.name == prevOp2.result.name) && (prevOp.result.name == prevOp3.result.name)) {
                        toRemove.add(i - 3)
                        toRemove.add(i - 2)
                        toRemove.add(i - 1)
                        toRemove.add(i)
                    }
                }
            }
            prevOp3 = prevOp2
            prevOp2 = prevOp
            prevOp = op
            i++
        }
        val newOperations = mutableListOf<Operation>()
        i = 0
        for (op in operations) {
            if (!toRemove.contains(i)) {
                newOperations.add(op)
            }
            i++
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