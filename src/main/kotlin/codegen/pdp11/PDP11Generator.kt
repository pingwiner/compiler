package org.pingwiner.compiler.codegen.pdp11

import org.pingwiner.compiler.codegen.Generator
import org.pingwiner.compiler.codegen.Operand
import org.pingwiner.compiler.codegen.OperandType
import org.pingwiner.compiler.codegen.Operation
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

    override fun generate(operations: List<Operation>): ByteArray {
        val pdpOperations = mutableListOf<Instruction>()
        val usage = getRegUsage(operations)
        for (i in 0..operations.size - 1) {
            val regs = usage.getUsedRegs(i)
            val sb = StringBuilder()
            for (r in regs) {
                sb.append(r)
                if (r != regs.last()) sb.append(", ")
            }
            println(sb.toString())
        }

        val result = byteArrayOf()
        var skipNext = false
        for ((i, op) in operations.withIndex()) {
            if (skipNext) {
                skipNext = false
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
                                        pdpOperations.add(create2OperandInstruction(op, 1))
                                        skipNext = true
                                        continue
                                    } else if ((op.operand2.name == nextOp.result.name) && (op.operand2.type == OperandType.LocalVariable)) {
                                        pdpOperations.add(create2OperandInstruction(op, 2))
                                        skipNext = true
                                        continue
                                    }
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
                is Operation.Goto -> TODO()
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

        return result
    }

    private fun create2OperandInstruction(operation: Operation, destination: Int): DoubleOperandInstruction {
        TODO()
    }

    fun assign(op: Operation.Assignment) {

    }


}