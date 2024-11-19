package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.Program

class Z80Generator(val program: Program) {
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

}