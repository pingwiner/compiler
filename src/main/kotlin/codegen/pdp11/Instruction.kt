package org.pingwiner.compiler.codegen.pdp11

enum class Mode(val value: Int) {
    // Direct addressing of the register
    Reg(0),

    // Contents of Reg is the address
    RegDef(1),

    // Contents of Reg is the address, then Reg incremented
    AutoInc(2),

    // Content of Reg is addr of addr, then Reg Incremented
    AutoIncDef(3),

    // Reg is decremented then contents is address
    AutoDec(4),

    // Reg is decremented then contents is addr of addr
    AutoDecDef(5),

    // Contents of Reg + Following word is address
    Index(6),

    // Contents of Reg + Following word is addr of addr
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

// Moves a value from source to destination.
class Mov(src: Operand, dst: Operand) : DoubleOperandInstruction("MOV", src, dst, false, 1)
class Movb(src: Operand, dst: Operand) : DoubleOperandInstruction("MOVB", src, dst, true, 1)

// Compares values by subtracting the destination from the source, setting the condition codes, and then discarding the result of the subtraction.
class Cmp(src: Operand, dst: Operand) : DoubleOperandInstruction("CMP", src, dst, false, 2)
class Cmpb(src: Operand, dst: Operand) : DoubleOperandInstruction("CMPB", src, dst, true, 2)

// Performs a bit-wise AND of the source and the destination, sets the condition codes, and then discards the result of the AND.
class Bit(src: Operand, dst: Operand) : DoubleOperandInstruction("BIT", src, dst, false, 3)
class Bitb(src: Operand, dst: Operand) : DoubleOperandInstruction("BIT", src, dst, true, 3)

// For each bit set in the source, that bit is cleared in the destination.
// This is accomplished by taking the ones-complement of the source and ANDing it with the destination. The result of the AND is stored in the destination.
class Bic(src: Operand, dst: Operand) : DoubleOperandInstruction("BIC", src, dst, false, 4)
class Bicb(src: Operand, dst: Operand) : DoubleOperandInstruction("BICB", src, dst, true, 4)

// For each bit set in the source, that bit is set in the destination.
// This is accomplished by ORing the source and destination, and storing the result in the destination.
class Bis(src: Operand, dst: Operand) : DoubleOperandInstruction("BIC", src, dst, false, 5)
class Bisb(src: Operand, dst: Operand) : DoubleOperandInstruction("BIC", src, dst, true, 5)

// Adds the source and destination, storing the results in the destination.
class Add(src: Operand, dst: Operand) : DoubleOperandInstruction("ADD", src, dst, false, 6)

// Subtracts the source from the destination, storing the results in the destination.
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

// Swap bytes
class Swab(dst: Operand) : SingleOperandInstruction("SWAB", dst, false, 0b000_011)

// Set all bits to zero
class Clr(dst: Operand) : SingleOperandInstruction("CLR", dst, false, 0b101_000)
class Clrb(dst: Operand) : SingleOperandInstruction("CLRB", dst, true, 0b101_000)

// Inversion
class Com(dst: Operand) : SingleOperandInstruction("COM", dst, false, 0b101_001)
class Comb(dst: Operand) : SingleOperandInstruction("COMB", dst, true, 0b101_001)

// Increment
class Inc(dst: Operand) : SingleOperandInstruction("INC", dst, false, 0b101_010)
class Incb(dst: Operand) : SingleOperandInstruction("INCB", dst, true, 0b101_010)

// Decrement
class Dec(dst: Operand) : SingleOperandInstruction("DEC", dst, false, 0b101_011)
class Decb(dst: Operand) : SingleOperandInstruction("DECB", dst, true, 0b101_011)

// Negate (invert sign)
class Neg(dst: Operand) : SingleOperandInstruction("NEG", dst, false, 0b101_100)
class Negb(dst: Operand) : SingleOperandInstruction("NEGB", dst, true, 0b101_100)

// Adds the current value of the carry flag to the destination.
class Adc(dst: Operand) : SingleOperandInstruction("ADC", dst, false, 0b101_101)
class Adcb(dst: Operand) : SingleOperandInstruction("ADCB", dst, true, 0b101_101)

// Subtracts the current value of the carry flag from the destination.
class Sbc(dst: Operand) : SingleOperandInstruction("SBC", dst, false, 0b101_110)
class Sbcb(dst: Operand) : SingleOperandInstruction("SBCB", dst, true, 0b101_110)

// Sets the N (negative) and Z (zero) condition codes based on the value of the operand.
class Tst(dst: Operand) : SingleOperandInstruction("TST", dst, false, 0b101_111)
class Tstb(dst: Operand) : SingleOperandInstruction("TSTB", dst, true, 0b101_111)

// Rotates the bits of the operand one position to the right.
// The right-most bit is placed in the carry flag, and the carry flag is copied to the left-most bit (bit 15) of the operand.
class Ror(dst: Operand) : SingleOperandInstruction("ROR", dst, false, 0b110_000)
class Rorb(dst: Operand) : SingleOperandInstruction("RORB", dst, true, 0b110_000)

// Rotates the bits of the operand one position to the left.
// The left-most bit is placed in the carry flag, and the carry flag is copied to the right-most bit (bit 0) of the operand.
class Rol(dst: Operand) : SingleOperandInstruction("ROL", dst, false, 0b110_001)
class Rolb(dst: Operand) : SingleOperandInstruction("ROLB", dst, true, 0b110_001)

// Shifts the bits of the operand one position to the right.
// The left-most bit is duplicated. The effect is to perform a signed division by 2.
class Asr(dst: Operand) : SingleOperandInstruction("ASR", dst, false, 0b110_010)
class Asrb(dst: Operand) : SingleOperandInstruction("ASRB", dst, true, 0b110_010)

// Shifts the bits of the operand one position to the left.
// The right-most bit is set to zero. The effect is to perform a signed multiplication by 2.
class Asl(dst: Operand) : SingleOperandInstruction("ASL", dst, false, 0b110_011)
class Aslb(dst: Operand) : SingleOperandInstruction("ASLB", dst, true, 0b110_011)

// MARK is used as part of one of the subroutine call/ return sequences. The operand is the number of parameters
// SP <- PC + 2 * n; PC <- R5; R5 <- (SP)+
class Mark(dst: Operand) : SingleOperandInstruction("MARK", dst, false, 0b110_100)

// MTPS is only on LSI-11s, and is used to move a byte to the processor status word.
class Mtps(dst: Operand) : SingleOperandInstruction("MTPS", dst, true, 0b110_100)

// Pushes a word onto the current R6 stack from the operand address in the previous address space, as indicated in the PSW.
class Mfpi(dst: Operand) : SingleOperandInstruction("MFPI", dst, false, 0b110_101)
class Mfpd(dst: Operand) : SingleOperandInstruction("MFPD", dst, true, 0b110_101)

// Pops a word from the current stack as indicated in the PSW to the operand address in the previous address space, as indicated in the PSW
class Mtpi(dst: Operand) : SingleOperandInstruction("MTPI", dst, false, 0b110_110)
class Mtpd(dst: Operand) : SingleOperandInstruction("MTPD", dst, true, 0b110_110)

// SXT sets the destination to zero if the N (negative) flag is clear, or to all ones if N is set.
class Sxt(dst: Operand) : SingleOperandInstruction("SXT", dst, false, 0b110_111)

// MFPS copies the processor status byte to the indicated register. This only exists on LSI-11s,
class Mfps(dst: Operand) : SingleOperandInstruction("MFPS", dst, true, 0b110_111)


open class BranchInstruction(name: String, val opcode: Int, val dst: Int) : Instruction(name) {
    override fun value(): Int {
        return ((opcode and 0xff) shl 8) or (dst and 0xff)
    }
}

// Branch always
class Br(dst: Int) : BranchInstruction("BR", 0b0000_0001, dst)

// Branch if not equal (Z == 0)
class Bne(dst: Int) : BranchInstruction("BNE", 0b0000_0010, dst)

// Branch if equal (Z == 1)
class Beq(dst: Int) : BranchInstruction("BEQ", 0b0000_0011, dst)

// Branch if greater or equal (N & V) == 0
class Bge(dst: Int) : BranchInstruction("BGE", 0b0000_0100, dst)

// Branch if less than (N & V) == 1
class Blt(dst: Int) : BranchInstruction("BLT", 0b0000_0101, dst)

// Branch if greater than (Z | (N & V)) == 0
class Bgt(dst: Int) : BranchInstruction("BGT", 0b0000_0110, dst)

// Branch if less or equal (Z | (N & V)) == 1
class Ble(dst: Int) : BranchInstruction("BLE", 0b0000_0111, dst)

// Branch if plus (N == 0)
class Bpl(dst: Int) : BranchInstruction("BPL", 0b1000_0000, dst)

// Branch if minus (N == 1)
class Bmi(dst: Int) : BranchInstruction("BMI", 0b1000_0001, dst)

// Branch if higher (C == 0 and Z == 0)
class Bhi(dst: Int) : BranchInstruction("BHI", 0b1000_0010, dst)

// Branch if lower or same (C | Z == 1)
class Blos(dst: Int) : BranchInstruction("BLOS", 0b1000_0011, dst)

// Branch if overflow clear
class Bvc(dst: Int) : BranchInstruction("BVC", 0b1000_0100, dst)

// Branch if overflow set
class Bvs(dst: Int) : BranchInstruction("BVS", 0b1000_0101, dst)

// Branch if carry clear
class Bcc(dst: Int) : BranchInstruction("BCC", 0b1000_0110, dst)

// Branch if carry set
class Bcs(dst: Int) : BranchInstruction("BCS", 0b1000_0111, dst)