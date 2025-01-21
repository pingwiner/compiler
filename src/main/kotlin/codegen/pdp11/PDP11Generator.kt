package org.pingwiner.compiler.codegen.pdp11

import org.pingwiner.compiler.codegen.*
import org.pingwiner.compiler.codegen.Operand
import org.pingwiner.compiler.parser.Program

class PDP11Generator(program: Program) : Generator(program) {
    private val baseAddr = oct(1000)
    private var nextRegNumber = 0
    private val squashedRegisters = mutableMapOf<String, String>()
    private val lirOperations = mutableListOf<LirInstruction>()
    private val functions = mutableMapOf<String, LirFunction>()
    private val usedFunctions = mutableSetOf("main")

    private fun allocReg(): LirOperand {
        nextRegNumber++
        val regName = "%%$nextRegNumber"
        return LirOperand(LirOperandType.Register, regName, 0)
    }

    private fun printGlobalVariables(): String {
        val sb = StringBuilder()
        for (v in program.globalVars.values) {
            if (v.isExtern) continue
            if (v.size == 1) {
                sb.append("${v.name}:\n    .WORD ${v.value?.get(0)?.asOctal() ?: "0"}")
            } else {
                sb.append("${v.name}:\n    .WORD ")
                for (i in 0..<v.size) {
                    sb.append("${v.value?.get(i)?.asOctal() ?: 0}")
                    if (i != v.size - 1) {
                        sb.append(", ")
                    }
                }
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun Operand.toLirOp() : LirOperand {
        return when(this.type) {
            OperandType.Register -> {
                if (squashedRegisters.containsKey(this.name)) {
                    LirOperand(LirOperandType.LocalVar, squashedRegisters[this.name]!!, 0)
                } else {
                    LirOperand(LirOperandType.Register, this.name, 0)
                }
            }
            OperandType.ImmediateValue -> LirOperand(LirOperandType.Immediate, "", this.value ?: 0)
            OperandType.LocalVariable -> LirOperand(LirOperandType.LocalVar, this.name, this.value ?: 0)
            OperandType.GlobalVariable -> LirOperand(LirOperandType.GlobalVar, this.name, 0)
            else -> {
                throw IllegalArgumentException("Unexpected operand")
            }
        }
    }

    private fun squashSsaAssignments(operations: List<Operation>): List<Operation> {
        var regValMap = mutableMapOf<String, String>()
        var i = 0
        var skipNextOp = false
        val result = mutableListOf<Operation>()
        for (op in operations) {
            if (skipNextOp) {
                i++
                skipNextOp = false
                continue
            }
            if (op != operations.last()) {
                if (op is Operation.BinaryOperation) {
                    if (!op.operator.isCondition() && (op.operator != Operator.XOR)) {
                        if (op.result.type == OperandType.Register) {
                            val nextOp = operations[i + 1]
                            if (nextOp is Operation.Assignment) {
                                if (nextOp.operand.type == OperandType.Register) {
                                    if (nextOp.operand.name == op.result.name) {
                                        if ((op.operand1.name == nextOp.result.name) && (op.operand1.type == OperandType.LocalVariable)) {
                                            regValMap[op.result.name] = op.operand1.name
                                            val newOp = Operation.BinaryOperation(
                                                op.operand1,
                                                op.operand1,
                                                op.operand2,
                                                op.operator
                                            )
                                            result.add(checkOperand2(newOp, regValMap))
                                            squashedRegisters[op.result.name] = newOp.result.name
                                            i++
                                            skipNextOp = true
                                            continue
                                        } else if ((op.operand2.name == nextOp.result.name) && (op.operand2.type == OperandType.LocalVariable)) {
                                            if (op.operator.isCommutative()) {
                                                regValMap[op.result.name] = op.operand2.name
                                                val newOp = Operation.BinaryOperation(
                                                    op.operand2,
                                                    op.operand2,
                                                    op.operand1,
                                                    op.operator
                                                )
                                                result.add(checkOperand2(newOp, regValMap))
                                                i++
                                                skipNextOp = true
                                                continue
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val newOp1 = checkOperand1(op, regValMap)
                    val newOp2 = checkOperand2(newOp1, regValMap)
                    result.add(newOp2)
                    i++
                    continue
                }
                if (op is Operation.Assignment) {
                    if ((op.operand.type == OperandType.Register) && (op.result.type == OperandType.LocalVariable)) {
                        if (!checkIfVarUsed(op.result.name,  operations.subList(i + 1, operations.size))) {
                            i++
                            continue
                        }
                    }
                }
            }
            result.add(op)
            i++
        }
        return result
    }

    private fun checkIfVarUsed(varName: String, ops: List<Operation>): Boolean {
        for (op in ops) {
            when (op) {
                is Operation.Return -> {
                    if (op.result.name == varName) return true
                }
                is Operation.Assignment -> {
                    if (op.operand.name == varName) return true
                }
                is Operation.BinaryOperation -> {
                    if ((op.operand1.name == varName) || (op.operand2.name == varName)) return true
                }
                is Operation.Call -> {
                    if (op.args.any { it.name == varName }) return true
                }
                is Operation.Goto -> {}
                is Operation.IfNot -> {
                    if (op.condition.name == varName) return true
                }
                is Operation.Inv -> {
                    if (op.operand.name == varName) return true
                }
                is Operation.Label -> {}
                is Operation.Load -> {
                    if (op.index.name == varName) return true
                }
                is Operation.Neg -> {
                    if (op.operand.name == varName) return true
                }
                is Operation.SetResult -> {
                    if (op.result.name == varName) return true
                }
                is Operation.Store -> {
                    if (op.index.name == varName) return true
                }
            }
        }
        return false
    }

    private fun checkOperand1(op: Operation.BinaryOperation, regMap: Map<String, String>): Operation.BinaryOperation {
        if (op.operand1.type == OperandType.Register) {
            if (regMap.containsKey(op.operand1.name)) {
                val varName = regMap[op.operand1.name]
                return Operation.BinaryOperation(op.result, Operand(varName!!, OperandType.LocalVariable, 0), op.operand2, op.operator)
            }
        }
        return op
    }

    private fun checkOperand2(op: Operation.BinaryOperation, regMap: Map<String, String>): Operation.BinaryOperation {
        if (op.operand2.type == OperandType.Register) {
            if (regMap.containsKey(op.operand2.name)) {
                val varName = regMap[op.operand2.name]
                return Operation.BinaryOperation(op.result, op.operand1, Operand(varName!!, OperandType.LocalVariable, 0), op.operator)
            }
        }
        return op
    }

    private fun printLirOperations(operations: List<LirInstruction>) {
        for (op in operations) {
            if (op is LirLabel) {
                println("\n" + op.toString())
            } else {
                println("    $op")
            }
        }
    }

    override fun addFunction(name: String, irOperations: List<Operation>) {
        val operations = squashSsaAssignments(irOperations)

        lirOperations.clear()

        var skipNext = false
        for ((i, op) in operations.withIndex()) {
            if (skipNext) {
                skipNext = false
                continue
            }
            // Handle conditional jumps
            if (op != operations.last()) {
                if (op is Operation.BinaryOperation) {
                    if ((op.result.type == OperandType.Register) && (op.operator.isCondition())) {
                        val nextOp = operations[i + 1]
                        if (nextOp is Operation.IfNot) {
                            if (nextOp.condition.type == OperandType.Register) {
                                if (nextOp.condition.name == op.result.name) {
                                    lirOperations.add(LirCmp(op.operand1.toLirOp(), op.operand2.toLirOp()))
                                    lirOperations.add(jmpIfNot(op.operator, nextOp.result))
                                    skipNext = true
                                    continue
                                }
                            }
                        }
                    }
                }
            }
            when (op) {
                is Operation.Assignment -> {
                    if ((op.operand.type == OperandType.ImmediateValue) && (op.operand.value == 0)) {
                        // Replace MOV #0, Rx by BIC Rx, Rx to reduce size
                        lirOperations.add(LirBic(op.result.toLirOp(), op.result.toLirOp()))
                    } else {
                        lirOperations.add(LirMov(op.operand.toLirOp(), op.result.toLirOp()))
                    }
                }
                is Operation.BinaryOperation -> create2OperandInstruction(op)
                is Operation.Call -> makeCall(op)
                is Operation.Goto -> {
                    lirOperations.add(LirJmp(op.result.name))
                }
                is Operation.IfNot -> {
                    lirOperations.add(LirCmp(op.condition.toLirOp(), Operand("", OperandType.ImmediateValue, 0).toLirOp()))
                    lirOperations.add(LirJe(op.result.name))
                }
                is Operation.Inv -> {
                    lirOperations.add(LirCom(op.operand.toLirOp()))
                }
                is Operation.Label -> {
                    lirOperations.add(LirLabel(op.result.name))
                }
                is Operation.Load -> {
                    val r = op.result.toLirOp()
                    if (op.index.type == OperandType.ImmediateValue) {
                        lirOperations.add(
                            LirMov(
                                LirOperand(
                                    type = LirOperandType.GlobalVar,
                                    name = "${op.base.name} + ${op.index.value!! * 2}",
                                    value = 0),
                                r
                            )
                        )
                    } else {
                        lirOperations.add(LirMov(op.index.toLirOp(), r))
                        lirOperations.add(LirAsl(r))
                        lirOperations.add(
                            LirMov(
                                LirOperand(
                                    type = LirOperandType.Indexed,
                                    name = r.name,
                                    value = 0,
                                    valueStr = op.base.name),
                                r
                            )
                        )
                    }
                }
                is Operation.Neg -> {
                    lirOperations.add(LirNeg(op.operand.toLirOp()))
                }
                is Operation.Return -> {
                    lirOperations.add(LirMov(op.result.toLirOp(), Operand("R0", OperandType.Register, 0).toLirOp()))
                    lirOperations.add(LirRet())
                }
                is Operation.SetResult -> {
                    lirOperations.add(LirMov(op.result.toLirOp(), LirOperand(LirOperandType.Register, "R0", 0)))
                }
                is Operation.Store -> {
                    val r = allocReg()
                    if (op.index.type == OperandType.ImmediateValue) {
                        lirOperations.add(
                            LirMov(
                                op.result.toLirOp(),
                                LirOperand(
                                    type = LirOperandType.GlobalVar,
                                    name = "${op.base.name} + ${op.index.value!! * 2}",
                                    value = 0)
                            )
                        )
                    } else {
                        lirOperations.add(LirMov(op.index.toLirOp(), r))
                        lirOperations.add(LirAsl(r))
                        lirOperations.add(
                            LirMov(
                                op.result.toLirOp(),
                                LirOperand(
                                    type = LirOperandType.Indexed,
                                    name = r.name,
                                    value = 0,
                                    valueStr = op.base.name)
                            )
                        )
                    }
                }
            }
        }

        val (usedRegs, usedVars) = reduceRegUsage(lirOperations)
        val instructions = mutableListOf<LirInstruction>()
        if (lirOperations.last() !is LirRet) {
            lirOperations.add(LirRet())
        }
        removeUselessMovInTheEndOfFunction()
        instructions.addAll(lirOperations)
        val localVars = mutableListOf<String>()
        val params = program.functions.find{ it.name == name }?.params
        params?.let {
            localVars.addAll(it)
        }
        val uniqVars = usedVars.filter { !localVars.contains(it) }
        // Reserve stack space for local vars
        if (uniqVars.isNotEmpty()) {
            localVars.addAll(uniqVars)
            instructions.add(1, LirReserve(uniqVars.size))
        }

        // Insert stack align before every ret instruction
        if (uniqVars.isNotEmpty()) {
            val newInstructions = mutableListOf<LirInstruction>()
            for (instruction in instructions) {
                if (instruction is LirRet) {
                    newInstructions.add(LirAlign(uniqVars.size))
                }
                newInstructions.add(instruction)
            }
            instructions.clear()
            instructions.addAll(newInstructions)
        }

        functions[name] = LirFunction(
            name,
            instructions,
            usedRegs,
            localVars
        )
    }

    private fun lookupForFunctionCall(instructions: List<LirInstruction>, index: Int): String {
        var i = index
        while(instructions[i] !is LirCall) i++
        return (instructions[i] as LirCall).label
    }

    private fun replaceLocalVars(lirFunction: LirFunction) {
        fun replaceOperand(operand: LirOperand): LirOperand {
            if (operand.type != LirOperandType.LocalVar) return operand
            var offset = lirFunction.getLocalVarOffset(operand.name) + operand.value
            if (program.functions.find { it.name == lirFunction.name }?.params?.contains(operand.name) == true) {
               offset += 2
            }
            if (offset == 0) {
                return LirOperand(
                    name = "SP",
                    type = LirOperandType.Indirect,
                    value = 0
                )
            }
            return LirOperand(
                name = "SP",
                type = LirOperandType.Indexed,
                value = offset
            )
        }

        for (op in lirFunction.instructions) {
            when (op) {
                is DoubleOpInstruction -> {
                    op.src = replaceOperand(op.src)
                    op.dst = replaceOperand(op.dst)
                }

                is SingleOpInstruction -> {
                    op.op = replaceOperand(op.op)
                }

                is LirJmpAbs -> {
                    op.op = replaceOperand(op.op)
                }

                is LirSob -> {
                    if (op.reg.type != LirOperandType.Register) {
                        throw IllegalArgumentException(op.reg.name + " must be register")
                    }
                }

                is LirPush -> {
                    op.op = replaceOperand(op.op)
                }

                is LirPop -> {
                    if (op.op.type != LirOperandType.Register) {
                        throw IllegalArgumentException(op.op.name + " must be register")
                    }
                }
            }
        }
    }

    private fun canDiscard(name: String, instructions: List<LirInstruction>, startIndex: Int): Boolean {
        var afterCall = false
        fun check(operand: LirOperand): Boolean {
            return operand.usesRegister() && (operand.name == name)
        }

        for (i in startIndex..<instructions.size) {
            val op = instructions[i]
            when (op) {
                is DoubleOpInstruction -> {
                    if (check(op.src)) return false
                    if (check(op.dst)) return true
                }

                is SingleOpInstruction -> {
                    if (check(op.op)) return false
                }

                is LirJmpAbs -> {
                    if (check(op.op)) return false
                }

                is LirSob -> {
                    if (check(op.reg)) return false
                }

                is LirPush -> {
                    if (afterCall) {
                        if (check(op.op)) return false
                    }
                }

                is LirPop -> {
                    if (check(op.op)) return false
                }

                is LirCall -> {
                    afterCall = true
                }
            }
        }
        return true
    }

    private fun replacePushPopRegs() {
        for (function in functions.values) {
            val newInstructionList = mutableListOf<LirInstruction>()
            val stack = ArrayDeque<String>()
            val regNames = setOf("R1", "R2", "R3", "R4", "R5")

            for ((index, instruction) in function.instructions.withIndex()) {
                if (instruction is LirPushRegs) {
                    val functionName = lookupForFunctionCall(function.instructions, index)
                    // Don't save R0 because it's used for return value
                    val regsToPush = functions[functionName]?.usedRegisters?.filter { regNames.contains(it) }
                    stack.clear()
                    regsToPush?.let {
                        for (reg in it) {
                            if (!canDiscard(reg, function.instructions, index + 1)) {
                                newInstructionList.add(LirPush(LirOperand(LirOperandType.Register, reg, 0)))
                                stack.addLast(reg)
                            }
                        }
                    }
                } else if (instruction is LirPopRegs) {
                    while(stack.size > 0) {
                        val reg = stack.removeLast()
                        newInstructionList.add(LirPop(LirOperand(LirOperandType.Register, reg, 0)))
                    }
                } else {
                    newInstructionList.add(instruction)
                }
            }
            function.instructions = newInstructionList
        }
    }

    override fun generateAssemblyCode(): String {
        replacePushPopRegs()
        println(".LINK ${baseAddr.asOctal()}\n")
        for (func in functions.values.filter { usedFunctions.contains(it.name) }.sortedBy { it.name != "main" }) {
            func.instructions = removeUselessMovs(func.instructions)
            replaceLocalVars(func)
            printLirOperations(func.instructions)
        }
        println(printGlobalVariables())
        return ""
    }

    private fun makeCall(op: Operation.Call) {
        usedFunctions.add(op.label.name)
        lirOperations.add(LirPushRegs())
        var offset = 0
        for (arg in op.args) {
            if (arg.type == OperandType.LocalVariable) {
                val newArg = Operand(
                    arg.name,
                    OperandType.LocalVariable,
                    offset
                )
                offset += 2
                lirOperations.add(LirPush(newArg.toLirOp()))
            } else {
                lirOperations.add(LirPush(arg.toLirOp()))
            }
        }
        lirOperations.add(LirCall(op.label.name))
        if (op.args.isNotEmpty()) {
            lirOperations.add(LirAlign(op.args.size))
        }
        lirOperations.add(LirPopRegs())
        lirOperations.add(LirMov(Operand("R0", OperandType.Register, 0).toLirOp(), op.result.toLirOp()))
    }

    private fun jmpIfNot(operator: Operator, label: Operand): LirInstruction {
        if (label.type != OperandType.Label) {
            throw IllegalArgumentException("Label expected")
        }
        return when (operator) {
            Operator.EQ -> LirJne(label.name)
            Operator.NEQ -> LirJe(label.name)
            Operator.LT -> LirJge(label.name)
            Operator.GT -> LirJle(label.name)
            Operator.GTEQ -> LirJlt(label.name)
            Operator.LTEQ -> LirJgt(label.name)
            else -> {
                throw IllegalArgumentException("Condition expected")
            }
        }
    }

    private fun addOperationXTimes(instr: LirInstruction, times: Int) {
        for (i in 0..<times) {
            lirOperations.add(instr)
        }
    }

    private fun create2OperandInstruction(operation: Operation.BinaryOperation) {
        if ((operation.operand1.name == operation.result.name) &&
            (operation.operand1.type == OperandType.LocalVariable) &&
            (operation.operator != org.pingwiner.compiler.codegen.Operator.MULTIPLY) &&
            (operation.operator != org.pingwiner.compiler.codegen.Operator.DIVIDE) &&
            (operation.operator != org.pingwiner.compiler.codegen.Operator.MOD)) {
            makeInPlaceBinaryOperation(operation)
        } else {
            val dst = operation.result.toLirOp()
            when (operation.operator) {
                Operator.PLUS -> {
                    lirOperations.add(LirMov(operation.operand1.toLirOp(), dst))
                    lirOperations.add(LirAdd(operation.operand2.toLirOp(), dst))
                }

                Operator.MINUS -> {
                    lirOperations.add(LirMov(operation.operand1.toLirOp(), dst))
                    lirOperations.add(LirSub(operation.operand2.toLirOp(), dst))
                }

                Operator.MULTIPLY -> {
                    callExtFun(
                        "_mul",
                        operation.operand1.toLirOp(),
                        operation.operand2.toLirOp(),
                        dst
                    )
                }
                Operator.DIVIDE -> {
                    callExtFun(
                        "_div",
                        operation.operand1.toLirOp(),
                        operation.operand2.toLirOp(),
                        dst
                    )
                }
                Operator.SHR,
                Operator.SHL -> {
                    makeShift(Operation.BinaryOperation(operation.result, operation.operand1, operation.operand2, operation.operator))
                }

                Operator.OR -> {
                    lirOperations.add(LirMov(operation.operand1.toLirOp(), dst))
                    lirOperations.add(LirBis(operation.operand2.toLirOp(), dst))
                }

                Operator.AND -> {
                    lirOperations.add(LirMov(operation.operand1.toLirOp(), dst))
                    val op2 = operation.operand2.toLirOp()
                    lirOperations.add(LirCom(op2))
                    lirOperations.add(LirBic(op2, dst))
                    lirOperations.add(LirCom(op2))
                }

                Operator.XOR -> {
                    val tempReg = allocReg()
                    lirOperations.add(LirMov(operation.operand1.toLirOp(), dst))
                    lirOperations.add(LirMov(operation.operand2.toLirOp(), tempReg))
                    lirOperations.add(LirXor(tempReg, dst))
                }
                Operator.MOD -> {
                    callExtFun(
                        "_mod",
                        operation.operand1.toLirOp(),
                        operation.operand2.toLirOp(),
                        dst
                    )
                }
                else -> {}
            }
        }
    }

    private fun callExtFun(name: String, op1: LirOperand, op2: LirOperand, dst: LirOperand) {
        usedFunctions.add(name)
        var offset = 0
        fun addOp(op: LirOperand) {
            if (op.type == LirOperandType.LocalVar) {
                val newArg = op.copy(
                    value = offset
                )
                offset += 2
                lirOperations.add(LirPush(newArg))
            } else {
                lirOperations.add(LirPush(op))
            }
        }
        addOp(op1)
        addOp(op2)
        lirOperations.add(LirCall(name))
        lirOperations.add(LirAlign(2))
        lirOperations.add(LirMov(LirOperand(LirOperandType.Register, "R0", 0), dst))
    }

    private fun removeUselessMovs(instructions: List<LirInstruction>): List<LirInstruction> =
        instructions.filter { (it !is LirMov) || (it.src != it.dst) }

    private fun makeInPlaceBinaryOperation(operation: Operation.BinaryOperation) {
        when(operation.operator) {
            Operator.PLUS -> {
                if ((operation.operand2.type == OperandType.ImmediateValue) && (operation.operand2.value == 1)) {
                    lirOperations.add(LirInc(operation.operand1.toLirOp()))
                } else {
                    lirOperations.add(LirAdd(operation.operand2.toLirOp(), operation.operand1.toLirOp()))
                }
            }
            Operator.MINUS -> {
                if ((operation.operand2.type == OperandType.ImmediateValue) && (operation.operand2.value == 1)) {
                    lirOperations.add(LirDec(operation.operand1.toLirOp()))
                } else {
                    lirOperations.add(LirSub(operation.operand2.toLirOp(), operation.operand1.toLirOp()))
                }
            }
            Operator.SHR -> makeShift(operation)
            Operator.SHL -> makeShift(operation)
            Operator.OR -> {
                lirOperations.add(LirBis(operation.operand2.toLirOp(), operation.operand1.toLirOp()))
            }
            Operator.AND -> {
                lirOperations.add(LirCom(operation.operand2.toLirOp()))
                lirOperations.add(LirBic(operation.operand2.toLirOp(), operation.operand1.toLirOp()))
                lirOperations.add(LirCom(operation.operand2.toLirOp()))
            }
            Operator.XOR -> { // should never happen
                throw IllegalStateException("Xor should not be inplace operation")
            }
            else -> {}
        }
    }

    private fun makeShift(operation: Operation.BinaryOperation) {
        if (operation.result != operation.operand1) {
            lirOperations.add(LirMov(operation.operand1.toLirOp(), operation.result.toLirOp()))
        }
        if ((operation.operand2.type == OperandType.ImmediateValue) && (operation.operand2.value != null) && (operation.operand2.value <= 3)) {
            val shift = operation.operand2.value
            if (shift > 0) {
                addOperationXTimes(
                    if (operation.operator == Operator.SHR)
                        LirAsr(operation.result.toLirOp())
                    else
                        LirAsl(operation.result.toLirOp()),
                    shift
                )
            }
        } else {
            val r = allocReg()
            lirOperations.add(LirMov(operation.operand2.toLirOp(), r))
            val label = "label" + lirOperations.size
            lirOperations.add(LirLabel(label))
            if (operation.operator == Operator.SHR)
                lirOperations.add(LirAsr(operation.result.toLirOp()))
            else
                lirOperations.add(LirAsl(operation.result.toLirOp()))
            lirOperations.add(LirSob(r, label))
        }
    }

    private fun removeUselessMovInTheEndOfFunction() {
        val size = lirOperations.size
        if (size < 3) return
        val toRemove = mutableSetOf<LirInstruction>()
        for (i in 0..size - 3) {
            val op1 = lirOperations[i]
            val op2 = lirOperations[i + 1]
            val op3 = lirOperations[i + 2]
            if (op3 !is LirRet) continue
            if ((op1 is LirMov) && (op2 is LirMov)) {
                if ((op1.src.type == LirOperandType.Register) && (op1.src.name == "R0") &&
                    (op1.dst == op2.src) && (op2.dst.type == LirOperandType.Register) &&
                    (op2.dst.name == "R0")
                ) {
                    toRemove.add(op1)
                    toRemove.add(op2)
                }
            }
        }
        lirOperations.removeAll { toRemove.contains(it) }
    }

}


