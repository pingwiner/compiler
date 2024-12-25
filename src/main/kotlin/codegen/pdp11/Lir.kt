package org.pingwiner.compiler.codegen.pdp11

enum class LirOperandType {
    immediate,
    localVar,
    globalVar,
    register
}

class LirOperand(
    val type: LirOperandType,
    val name: String,
    val value: Int
)

abstract class LirInstruction

class LirMov(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirAdd(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirSub(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirCmp(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirBit(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirBic(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirBis(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirXor(val src: LirOperand, val dst: LirOperand) : LirInstruction()

abstract class SingleOpInstruction(val op: LirOperand) : LirInstruction()

class LirSwab(op: LirOperand): SingleOpInstruction(op)
class LirClr(op: LirOperand): SingleOpInstruction(op)
class LirCom(op: LirOperand): SingleOpInstruction(op)
class LirInc(op: LirOperand): SingleOpInstruction(op)
class LirDec(op: LirOperand): SingleOpInstruction(op)
class LirNeg(op: LirOperand): SingleOpInstruction(op)
class LirAdc(op: LirOperand): SingleOpInstruction(op)
class LirSbc(op: LirOperand): SingleOpInstruction(op)
class LirTst(op: LirOperand): SingleOpInstruction(op)
class LirRor(op: LirOperand): SingleOpInstruction(op)
class LirRol(op: LirOperand): SingleOpInstruction(op)
class LirAsr(op: LirOperand): SingleOpInstruction(op)
class LirAsl(op: LirOperand): SingleOpInstruction(op)

class LirJmp(label: String): LirInstruction()
class LirJe(label: String): LirInstruction()
class LirJne(label: String): LirInstruction()
class LirJgt(label: String): LirInstruction()
class LirJlt(label: String): LirInstruction()
class LirJge(label: String): LirInstruction()
class LirJle(label: String): LirInstruction()
class LirJpl(label: String): LirInstruction()
class LirJmi(label: String): LirInstruction()

class LirJmpAbs(op: Operand): LirInstruction()

class LirCall(label: String): LirInstruction()
class LirRet(): LirInstruction()
class LirPush(op: LirOperand): LirInstruction()
class LirPop(op: LirOperand): LirInstruction()
class LirPushRegs(): LirInstruction()
class LirPopRegs(): LirInstruction()
class LirAlign(val size: Int): LirInstruction()