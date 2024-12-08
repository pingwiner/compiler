package org.pingwiner.compiler.codegen.pdp11

import org.pingwiner.compiler.codegen.Generator
import org.pingwiner.compiler.codegen.Operand
import org.pingwiner.compiler.codegen.OperandType
import org.pingwiner.compiler.codegen.Operation
import org.pingwiner.compiler.parser.Program
import kotlin.jvm.Throws

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

    private fun checkOperand(operand: Operand, index: Int, map: MutableMap<String, Int>) {
        if (operand.type == OperandType.Register) {
            map[operand.name] = index
        }
    }

    fun regUsage(operations: List<Operation>) : Map<String, Int> {
        var i = 0
        val result = mutableMapOf<String, Int>()
        for (op in operations) {
            when (op) {
                is Operation.BinaryOperation -> {
                    checkOperand(op.operand1, i, result)
                    checkOperand(op.operand2, i, result)
                }
                is Operation.Assignment -> {
                    checkOperand(op.operand, i, result)
                }
                is Operation.Call -> {
                    for (arg in op.args) {
                        checkOperand(arg, i, result)
                    }
                }
                is Operation.Goto -> {}
                is Operation.IfNot -> {
                    checkOperand(op.condition, i, result)
                }
                is Operation.Inv -> {
                    checkOperand(op.operand, i, result)
                }
                is Operation.Label -> {}
                is Operation.Load -> {
                    checkOperand(op.index, i, result)
                }
                is Operation.Neg -> {
                    checkOperand(op.operand, i, result)
                }
                is Operation.Return -> {
                    checkOperand(op.result, i, result)
                }
                is Operation.SetResult -> {
                    checkOperand(op.result, i, result)
                }
                is Operation.Store -> {
                    checkOperand(op.index, i, result)
                }
            }
            i++
        }
        return result
    }

    override fun generate(operations: List<Operation>): ByteArray {
        val result = byteArrayOf()
        for (op in operations) {
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

    fun assign(op: Operation.Assignment) {

    }


}