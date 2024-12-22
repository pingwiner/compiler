package org.pingwiner.compiler.codegen.pdp11

import org.pingwiner.compiler.codegen.*
import org.pingwiner.compiler.codegen.Operand
import org.pingwiner.compiler.parser.Program

class PDP11Generator(program: Program) : Generator(program) {
    private val globalVarsAllocMap = mutableMapOf<String, Int>()
    private val baseAddr = 0x3000

    private fun allocVars() {
        var addr =baseAddr
        for(v in program.globalVars.values) {
            globalVarsAllocMap[v.name] = addr
            addr += v.size * 2
        }
    }

    private fun getParamOffset(funcName: String, paramName: String): Int {
        val func = program.functions.firstOrNull{ it.name == funcName} ?: throw IllegalArgumentException("No such function: $funcName")
        var offset = func.vars.size * 2
        for(param in func.params) {
            if (param == paramName) {
                return offset
            }
            offset += 2
        }
        throw IllegalArgumentException("Function $funcName has no parameter $paramName")
    }

    private fun getLocalVarOffset(funcName: String, varName: String): Int {
        val func = program.functions.firstOrNull{ it.name == funcName} ?: throw IllegalArgumentException("No such function: $funcName")
        var offset = 0
        for (v in func.vars) {
            if (v == varName) {
                return offset
            }
            offset += 2
        }
        throw IllegalArgumentException("Function $funcName has no local variable $varName")
    }

    class RegUsage {
        private val data = mutableMapOf<String, Pair<Int, Int>>()

        fun create(op: Operand, index: Int) {
            if (op.type == OperandType.Register) {
                if (!data.containsKey(op.name)) {
                    data[op.name] = Pair(index, index)
                } else {
                    use(op, index)
                }
            }
        }
        
        fun use(op: Operand, index: Int) {
            if (op.type == OperandType.Register) {
                val oldVal = data[op.name]
                data[op.name] = oldVal!!.copy(second = index)
            }            
        }

        fun isRegUsed(name: String, index: Int): Boolean {
            val v = data[name] ?: return false
            return (v.first <= index) and (v.second >= index)
        }
        
        fun getUsedRegs(index: Int): Set<String> {
            val result = mutableSetOf<String>()
            val regs = data.keys
            for (reg in regs) {
                if (isRegUsed(reg, index)) {
                    result.add(reg)
                }
            }
            return result
        }
    }

    fun getRegUsage(operations: List<Operation>): RegUsage {
        var i = 0
        val usage = RegUsage()
        for (op in operations) {
            usage.create(op.result, i)
            when (op) {
                is Operation.BinaryOperation -> {
                    usage.use(op.operand1, i)
                    usage.use(op.operand2, i)
                }
                is Operation.Assignment -> {
                    usage.use(op.operand, i)
                }
                is Operation.Call -> {
                    for (arg in op.args) {
                        usage.use(arg, i)
                    }
                }
                is Operation.Goto -> {}
                is Operation.IfNot -> {
                    usage.use(op.condition, i)
                }
                is Operation.Inv -> {
                    usage.use(op.operand, i)
                }
                is Operation.Label -> {}
                is Operation.Load -> {
                    usage.use(op.index, i)
                }
                is Operation.Neg -> {
                    usage.use(op.operand, i)
                }
                is Operation.Return -> {
                    usage.use(op.result, i)
                }
                is Operation.SetResult -> {
                    usage.use(op.result, i)
                }
                is Operation.Store -> {
                    usage.use(op.index, i)
                }
            }
            i++
        }
        return usage
    }

    fun squashSsaAssignments(operations: List<Operation>): List<Operation> {
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
                    if (op.result.type == OperandType.Register) {
                        val nextOp = operations[i + 1]
                        if (nextOp is Operation.Assignment) {
                            if (nextOp.operand.type == OperandType.Register) {
                                if (nextOp.operand.name == op.result.name) {
                                    if ((op.operand1.name == nextOp.result.name) && (op.operand1.type == OperandType.LocalVariable)) {
                                        regValMap[op.result.name] = op.operand1.name
                                        val newOp = Operation.BinaryOperation(op.operand1, op.operand1, op.operand2, op.operator)
                                        result.add(checkOperand2(newOp, regValMap))
                                        i++
                                        skipNextOp = true
                                        continue
                                    } else if ((op.operand2.name == nextOp.result.name) && (op.operand2.type == OperandType.LocalVariable)) {
                                        if (op.operator.isCommutative()) {
                                            regValMap[op.result.name] = op.operand2.name
                                            val newOp = Operation.BinaryOperation(op.operand2, op.operand2, op.operand1, op.operator)
                                            result.add(checkOperand2(newOp, regValMap))
                                            i++
                                            skipNextOp = true
                                            continue
                                        }
                                    }
                                }
                            }
                        } else if (nextOp is Operation.IfNot) {

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

    val pdpOperations = mutableListOf<LirInstruction>()

    fun printRegUsage(operations: List<Operation>) {
        val usage = getRegUsage(operations)
        for (i in operations.indices) {
            val regs = usage.getUsedRegs(i)
            val sb = StringBuilder()
            for (r in regs) {
                sb.append(r)
                if (r != regs.last()) sb.append(", ")
            }
            println(sb.toString())
        }
    }

    override fun generate(operations: List<Operation>): ByteArray {
        pdpOperations.clear()

        var skipNext = false
        for ((i, op) in operations.withIndex()) {
            if (skipNext) {
                skipNext = false
                continue
            }
            if (op != operations.last()) {
                if (op is Operation.BinaryOperation) {
                    if ((op.result.type == OperandType.Register) && (op.operator.isCondition())) {
                        val nextOp = operations[i + 1]
                        if (nextOp is Operation.IfNot) {
                            if (nextOp.condition.type == OperandType.Register) {
                                if (nextOp.condition.name == op.result.name) {
                                    pdpOperations.add(LirCmp(op.operand1.toLirOp(), op.operand2.toLirOp()))
                                    pdpOperations.add(jmpIfNot(op.operator, nextOp.result))
                                    skipNext = true
                                    continue
                                }
                            }
                        }
                    }
                }
            }
            when (op) {
                is Operation.Assignment -> assign(op)
                is Operation.BinaryOperation -> TODO()
                is Operation.Call -> TODO()
                is Operation.Goto -> {
                    pdpOperations.add(LirJmp(0, op.result.name))
                }
                is Operation.IfNot -> TODO()
                is Operation.Inv -> TODO()
                is Operation.Label -> TODO()
                is Operation.Load -> TODO()
                is Operation.Neg -> TODO()
                is Operation.Return -> TODO()
                is Operation.SetResult -> TODO()
                is Operation.Store -> TODO()
            }
        }
        return ByteArray(0)
    }

    private fun jmpIfNot(operator: Operator, label: Operand): LirInstruction {
        if (label.type != OperandType.Label) {
            throw IllegalArgumentException("Label expected")
        }
        return when (operator) {
            Operator.EQ -> LirJne(0, label.name)
            Operator.NEQ -> LirJe(0, label.name)
            Operator.LT -> LirJge(0, label.name)
            Operator.GT -> LirJle(0, label.name)
            Operator.GTEQ -> LirJlt(0, label.name)
            Operator.LTEQ -> LirJgt(0, label.name)
            else -> {
                throw IllegalArgumentException("Condition expected")
            }
        }
    }

    private fun addOperationXTimes(instr: LirInstruction, times: Int) {
        for (i in 0..<times) {
            pdpOperations.add(instr)
        }
    }

    private fun create2OperandInstruction(operation: Operation.BinaryOperation) {
        val dst = operation.operand1.toLirOp()
        val src = operation.operand2.toLirOp()
        when(operation.operator) {
            Operator.PLUS -> { pdpOperations.add( LirAdd(src, dst) ) }
            Operator.MINUS -> { pdpOperations.add( LirSub(src, dst) ) }
            Operator.MULTIPLY -> TODO()
            Operator.DIVIDE -> TODO()
            Operator.SHR -> {
                for (i in 0..<src.value) {
                    pdpOperations.add(
                        LirAsr(dst)
                    )
                }
            }
            Operator.SHL -> {
                for (i in 0..<src.value) {
                    pdpOperations.add(
                        LirAsl(dst)
                    )
                }
            }
            Operator.OR -> { pdpOperations.add( LirBis(src, dst) ) }
            Operator.AND -> {
                pdpOperations.add( LirCom(src) )
                pdpOperations.add( LirBic(src, dst) )
                pdpOperations.add( LirCom(src) )
            }
            Operator.XOR -> TODO()
            Operator.MOD -> TODO()
            else -> {}
        }
    }

    fun assign(operation: Operation.Assignment) {
        pdpOperations.add(LirMov(operation.operand.toLirOp(), operation.result.toLirOp()))
    }


}

fun Operand.toLirOp() : LirOperand {
    return when(this.type) {
        OperandType.Register -> LirOperand(LirOperandType.register, this.name, 0)
        OperandType.ImmediateValue -> LirOperand(LirOperandType.immediate, "", this.value ?: 0)
        OperandType.LocalVariable -> LirOperand(LirOperandType.localVar, this.name, 0)
        OperandType.GlobalVariable -> LirOperand(LirOperandType.globalVar, this.name, 0)
        OperandType.Label -> TODO()
        OperandType.Phi -> TODO()
    }
}