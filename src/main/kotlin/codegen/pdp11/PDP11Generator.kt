package org.pingwiner.compiler.codegen.pdp11

import org.pingwiner.compiler.codegen.Generator
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