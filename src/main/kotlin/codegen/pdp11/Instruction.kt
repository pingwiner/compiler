package org.pingwiner.compiler.codegen.pdp11

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

open class DoubleOperandInstruction(name: String, val src: Operand, val dst: Operand, val byteOperation: Boolean, val opcode: Int) : Instruction(name) {
    override fun value(): Int {
        var result: Int = ((opcode and 7) shl 12) + (src.value() shl 6) or dst.value()
        if (byteOperation) {
            result = result or (1 shl 15)
        }
        return result
    }
}

class Mov(src: Operand, dst: Operand) : DoubleOperandInstruction("MOV", src, dst, false, 1)
class Movb(src: Operand, dst: Operand) : DoubleOperandInstruction("MOVB", src, dst, true, 1)
class Cmp(src: Operand, dst: Operand) : DoubleOperandInstruction("CMP", src, dst, false, 2)
class Cmpb(src: Operand, dst: Operand) : DoubleOperandInstruction("CMPB", src, dst, true, 2)
class Bit(src: Operand, dst: Operand) : DoubleOperandInstruction("BIT", src, dst, false, 3)
class Bitb(src: Operand, dst: Operand) : DoubleOperandInstruction("BIT", src, dst, true, 3)
class Bic(src: Operand, dst: Operand) : DoubleOperandInstruction("BIC", src, dst, false, 4)
class Bicb(src: Operand, dst: Operand) : DoubleOperandInstruction("BICB", src, dst, true, 4)
class Bis(src: Operand, dst: Operand) : DoubleOperandInstruction("BIC", src, dst, false, 5)
class Bisb(src: Operand, dst: Operand) : DoubleOperandInstruction("BIC", src, dst, true, 5)
class Add(src: Operand, dst: Operand) : DoubleOperandInstruction("ADD", src, dst, false, 6)
class Sub(src: Operand, dst: Operand) : DoubleOperandInstruction("SUB", src, dst, true, 6)


