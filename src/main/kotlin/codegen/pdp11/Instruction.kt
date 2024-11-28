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
        var result: Int = ((opcode and 7) shl 12) or (src.value() shl 6) or dst.value()
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

open class SingleOperandInstruction(name: String, val dst: Operand, val byteOperation: Boolean, val opcode: Int) : Instruction(name) {
    override fun value(): Int {
        var result: Int = ((opcode and 0b111_111) shl 6) or dst.value()
        if (byteOperation) {
            result = result or (1 shl 15)
        }
        return result
    }
}

class Swab(dst: Operand) : SingleOperandInstruction("SWAB", dst, false, 0b000_011)
class Clr(dst: Operand) : SingleOperandInstruction("CLR", dst, false, 0b101_000)
class Clrb(dst: Operand) : SingleOperandInstruction("CLRB", dst, true, 0b101_000)
class Com(dst: Operand) : SingleOperandInstruction("COM", dst, false, 0b101_001)
class Comb(dst: Operand) : SingleOperandInstruction("COMB", dst, true, 0b101_001)
class Inc(dst: Operand) : SingleOperandInstruction("INC", dst, false, 0b101_010)
class Incb(dst: Operand) : SingleOperandInstruction("INCB", dst, true, 0b101_010)
class Dec(dst: Operand) : SingleOperandInstruction("DEC", dst, false, 0b101_011)
class Decb(dst: Operand) : SingleOperandInstruction("DECB", dst, true, 0b101_011)
class Neg(dst: Operand) : SingleOperandInstruction("NEG", dst, false, 0b101_100)
class Negb(dst: Operand) : SingleOperandInstruction("NEGB", dst, true, 0b101_100)
class Adc(dst: Operand) : SingleOperandInstruction("ADC", dst, false, 0b101_101)
class Adcb(dst: Operand) : SingleOperandInstruction("ADCB", dst, true, 0b101_101)
class Sbc(dst: Operand) : SingleOperandInstruction("SBC", dst, false, 0b101_110)
class Sbcb(dst: Operand) : SingleOperandInstruction("SBCB", dst, true, 0b101_110)
class Tst(dst: Operand) : SingleOperandInstruction("TST", dst, false, 0b101_111)
class Tstb(dst: Operand) : SingleOperandInstruction("TSTB", dst, true, 0b101_111)
class Ror(dst: Operand) : SingleOperandInstruction("ROR", dst, false, 0b110_000)
class Rorb(dst: Operand) : SingleOperandInstruction("RORB", dst, true, 0b110_000)
class Rol(dst: Operand) : SingleOperandInstruction("ROL", dst, false, 0b110_001)
class Rolb(dst: Operand) : SingleOperandInstruction("ROLB", dst, true, 0b110_001)
class Asr(dst: Operand) : SingleOperandInstruction("ASR", dst, false, 0b110_010)
class Asrb(dst: Operand) : SingleOperandInstruction("ASRB", dst, true, 0b110_010)
class Asl(dst: Operand) : SingleOperandInstruction("ASL", dst, false, 0b110_011)
class Aslb(dst: Operand) : SingleOperandInstruction("ASLB", dst, true, 0b110_011)
class Mark(dst: Operand) : SingleOperandInstruction("MARK", dst, false, 0b110_100)
class Mtps(dst: Operand) : SingleOperandInstruction("MTPS", dst, true, 0b110_100)
class Mfpi(dst: Operand) : SingleOperandInstruction("MFPI", dst, false, 0b110_101)
class Mfpd(dst: Operand) : SingleOperandInstruction("MFPD", dst, true, 0b110_101)
class Mtpi(dst: Operand) : SingleOperandInstruction("MTPI", dst, false, 0b110_110)
class Mtpd(dst: Operand) : SingleOperandInstruction("MTPD", dst, true, 0b110_110)
class Sxt(dst: Operand) : SingleOperandInstruction("SXT", dst, false, 0b110_111)
class Mfps(dst: Operand) : SingleOperandInstruction("MFPS", dst, true, 0b110_111)





