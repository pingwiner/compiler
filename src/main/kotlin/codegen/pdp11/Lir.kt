package org.pingwiner.compiler.codegen.pdp11

enum class LirOperandType {
    immediate,
    localVar,
    globalVar,
    register,
    indirect
}

data class LirOperand(
    val type: LirOperandType,
    val name: String,
    val value: Int
) {
    override fun toString(): String {
        return when(type) {
            LirOperandType.immediate -> "$value"
            LirOperandType.indirect -> "[$name]"
            else -> name
        }
    }
}

abstract class LirInstruction

class LirMov(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "MOV $src, $dst"
    }
}
class LirAdd(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "ADD $src, $dst"
    }
}
class LirSub(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "SUB $src, $dst"
    }
}
class LirCmp(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "CMP $src, $dst"
    }
}
class LirBit(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "BIT $src, $dst"
    }
}
class LirBic(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "BIC $src, $dst"
    }
}
class LirBis(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "BIS $src, $dst"
    }
}
class LirXor(val src: LirOperand, val dst: LirOperand) : LirInstruction() {
    override fun toString(): String {
        return "XOR $src, $dst"
    }
}

abstract class SingleOpInstruction(val op: LirOperand) : LirInstruction()

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

class LirJmpAbs(val op: LirOperand): LirInstruction() {
    override fun toString(): String {
        return "JMP $op"
    }
}

class LirSob(val reg: LirOperand, val label: String) : LirInstruction() {
    override fun toString(): String {
        return "SOB $reg, $label"
    }
}

class LirCall(val label: String): LirInstruction() {
    override fun toString(): String {
        return "CALL $label"
    }
}
class LirRet(): LirInstruction() {
    override fun toString(): String {
        return "RET"
    }
}
class LirPush(val op: LirOperand): LirInstruction() {
    override fun toString(): String {
        return "PUSH $op"
    }
}
class LirPop(val op: LirOperand): LirInstruction() {
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
