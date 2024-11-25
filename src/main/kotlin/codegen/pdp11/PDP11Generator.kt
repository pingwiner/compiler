package org.pingwiner.compiler.codegen.pdp11

import org.pingwiner.compiler.codegen.Generator
import org.pingwiner.compiler.codegen.Operation
import org.pingwiner.compiler.parser.Program

class PDP11Generator(program: Program) : Generator(program) {

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

    fun assign(op: Operation) {

    }


}