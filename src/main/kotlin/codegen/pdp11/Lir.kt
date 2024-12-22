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

open class LirInstruction

class LirMov(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirAdd(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirSub(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirCmp(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirBit(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirBic(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirBis(val src: LirOperand, val dst: LirOperand) : LirInstruction()
class LirXor(val src: LirOperand, val dst: LirOperand) : LirInstruction()

class LirSwab(val op: LirOperand): LirInstruction()
class LirClr(val op: LirOperand): LirInstruction()
class LirCom(val op: LirOperand): LirInstruction()
class LirInc(val op: LirOperand): LirInstruction()
class LirDec(val op: LirOperand): LirInstruction()
class LirNeg(val op: LirOperand): LirInstruction()
class LirAdc(val op: LirOperand): LirInstruction()
class LirSbc(val op: LirOperand): LirInstruction()
class LirTst(val op: LirOperand): LirInstruction()
class LirRor(val op: LirOperand): LirInstruction()
class LirRol(val op: LirOperand): LirInstruction()
class LirAsr(val op: LirOperand): LirInstruction()
class LirAsl(val op: LirOperand): LirInstruction()

class LirJmp(offset: Int, label: String): LirInstruction()
class LirJe(offset: Int, label: String): LirInstruction()
class LirJne(offset: Int, label: String): LirInstruction()
class LirJgt(offset: Int, label: String): LirInstruction()
class LirJlt(offset: Int, label: String): LirInstruction()
class LirJge(offset: Int, label: String): LirInstruction()
class LirJle(offset: Int, label: String): LirInstruction()
class LirJpl(offset: Int, label: String): LirInstruction()
class LirJmi(offset: Int, label: String): LirInstruction()

class LirJmpAbs(op: Operand): LirInstruction()

class LirCall(op: Operand, label: String): LirInstruction()
class LirRet(): LirInstruction()
class LirPush(op: Operand): LirInstruction()
class LirPop(op: Operand): LirInstruction()