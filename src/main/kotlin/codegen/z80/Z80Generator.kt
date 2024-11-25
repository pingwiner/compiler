package org.pingwiner.compiler.codegen.z80

import org.pingwiner.compiler.codegen.Generator
import org.pingwiner.compiler.codegen.Operation
import org.pingwiner.compiler.parser.Program

class Z80Generator(program: Program) : Generator(program) {
    fun globalVarsBaseAddress(): UShort {
        return 0xE000u
    }

    fun getGlobalVarAddress(name: String): UShort {
        val varList = program.globalVars.values.toList()

        TODO()
    }

    fun getFuncParamOffset(name: String): Short {
        TODO()
    }

    fun getLocalVarOffset(name: String): Short {
        TODO()
    }

    override fun generate(operations: List<Operation>): ByteArray {
        TODO("Not yet implemented")
    }


}