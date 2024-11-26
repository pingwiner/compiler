package org.pingwiner.compiler.codegen.pdp11

import java.nio.ByteBuffer

enum class Mode(val value: Int) {
    Reg(0),
    RegDef(1),
    AutoInc(2),
    AutoIncDef(3),
    AutoDec(4),
    AutoDecDef(5),
    Index(6),
    IndexDef(7)
}

enum class Reg(val value: Int) {
    R0(0),
    R1(1),
    R2(2),
    R3(3),
    R4(4),
    R5(5),
    SP(6),
    PC(7)
}

data class Operand(val mode: Mode, val reg: Reg) {
    fun value(): Int = mode.value shl 3 + reg.value
}

abstract class Instruction(val name: String) {
    abstract fun value() : Int
    fun asWord(): UShort = value().toUShort()
}

class Mov(val src: Operand, val dst: Operand) : Instruction("MOV") {
    override fun value(): Int {
        return (1 shl 12) + (src.value() shl 6) or dst.value()
    }
}

class Movb(val src: Operand, val dst: Operand) : Instruction("MOVB") {
    override fun value(): Int {
        return (0b1001 shl 12) + (src.value() shl 6) or dst.value()
    }
}