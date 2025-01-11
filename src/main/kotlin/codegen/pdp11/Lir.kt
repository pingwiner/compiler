package org.pingwiner.compiler.codegen.pdp11

enum class LirOperandType {
    Immediate, // #1024
    LocalVar,  // a
    GlobalVar, // globA
    Register,  // R1
    Indirect,  // (R1)
    Absolute,  // 1024
    Indexed    // 1024(R1)
}

data class LirOperand(
    val type: LirOperandType,
    val name: String,
    val value: Int
) {
    override fun toString(): String {
        return when(type) {
            LirOperandType.Immediate -> "#$value"
            LirOperandType.Indirect -> "($name)"
            LirOperandType.Absolute -> "$value"
            LirOperandType.Indexed -> "$value($name)"
            else -> name
        }
    }

    fun usesRegister() =
        (type == LirOperandType.Register) ||
        (type == LirOperandType.Indexed) ||
        (type == LirOperandType.Indirect)

    companion object {
        fun absolute(address: Int): LirOperand {
            return LirOperand(LirOperandType.Absolute, "", address)
        }

        fun indexed(name: String, offset: Int): LirOperand {
            return LirOperand(LirOperandType.Indexed, name, offset)
        }
    }
}

abstract class LirInstruction

abstract class DoubleOpInstruction(var src: LirOperand, var dst: LirOperand) : LirInstruction()

class LirMov(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "MOV $src, $dst"
    }
}
class LirAdd(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "ADD $src, $dst"
    }
}
class LirSub(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "SUB $src, $dst"
    }
}
class LirCmp(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "CMP $src, $dst"
    }
}
class LirBit(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "BIT $src, $dst"
    }
}
class LirBic(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "BIC $src, $dst"
    }
}
class LirBis(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "BIS $src, $dst"
    }
}
class LirXor(src: LirOperand, dst: LirOperand) : DoubleOpInstruction(src, dst) {
    override fun toString(): String {
        return "XOR $src, $dst"
    }
}

abstract class SingleOpInstruction(var op: LirOperand) : LirInstruction()

class LirSwab(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "SWAB $op"
    }
}
class LirClr(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "CLR $op"
    }
}
class LirCom(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "COM $op"
    }
}
class LirInc(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "INC $op"
    }
}
class LirDec(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "DEC $op"
    }
}
class LirNeg(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "NEG $op"
    }
}
class LirAdc(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "ADC $op"
    }
}
class LirSbc(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "SBC $op"
    }
}
class LirTst(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "TST $op"
    }
}
class LirRor(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "ROR $op"
    }
}
class LirRol(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "ROL $op"
    }
}
class LirAsr(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "ASR $op"
    }
}
class LirAsl(op: LirOperand): SingleOpInstruction(op) {
    override fun toString(): String {
        return "ASL $op"
    }
}

class LirJmp(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JMP $label"
    }
}
class LirJe(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JE $label"
    }
}
class LirJne(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JNE $label"
    }
}
class LirJgt(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JGT $label"
    }
}
class LirJlt(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JLT $label"
    }
}
class LirJge(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JGE $label"
    }
}
class LirJle(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JLE $label"
    }
}
class LirJpl(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JPL $label"
    }
}
class LirJmi(val label: String): LirInstruction() {
    override fun toString(): String {
        return "JMI $label"
    }
}

class LirJmpAbs(var op: LirOperand): LirInstruction() {
    override fun toString(): String {
        return "JMP $op"
    }
}

class LirSob(var reg: LirOperand, val label: String) : LirInstruction() {
    override fun toString(): String {
        return "SOB $reg, $label"
    }
}

class LirCall(val label: String): LirInstruction() {
    override fun toString(): String {
        return "CALL $label"
    }
}
class LirRet: LirInstruction() {
    override fun toString(): String {
        return "RET"
    }
}
class LirPush(var op: LirOperand): LirInstruction() {
    override fun toString(): String {
        return "PUSH $op"
    }
}
class LirPop(var op: LirOperand): LirInstruction() {
    override fun toString(): String {
        return "POP $op"
    }
}

//Push all used registers
class LirPushRegs(): LirInstruction() {
    override fun toString(): String {
        return "PUSH ALL"
    }
}

//Pop all registers, saved by previous PushRegs
class LirPopRegs(): LirInstruction() {
    override fun toString(): String {
        return "POP ALL"
    }
}
class LirReserve(val size: Int): LirInstruction() {
    override fun toString(): String {
        return "SUB SP, ${size*2}"
    }
}
class LirAlign(val size: Int): LirInstruction() {
    override fun toString(): String {
        return "ADD SP, ${size*2}"
    }
}

class LirLabel(val label: String): LirInstruction() {
    override fun toString(): String {
        return "$label:"
    }
}
