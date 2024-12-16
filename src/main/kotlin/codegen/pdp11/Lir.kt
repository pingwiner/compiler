package org.pingwiner.compiler.codegen.pdp11

import org.pingwiner.compiler.codegen.Operand

enum class LirOperandType {
    immediate,
    localVar,
    globalVar,
    register
}

class LirOperand(
    val type: LirOperandType,
    name: String,
    value: Int
)

open class LirInstruction

class LirMov(val op1: LirOperand, val op2: LirOperand) : LirInstruction()
class LirAdd(val op1: LirOperand, val op2: LirOperand) : LirInstruction()
class LirSub(val op1: LirOperand, val op2: LirOperand) : LirInstruction()
class LirCmp(val op1: LirOperand, val op2: LirOperand) : LirInstruction()
class LirBit(val op1: LirOperand, val op2: LirOperand) : LirInstruction()
class LirBic(val op1: LirOperand, val op2: LirOperand) : LirInstruction()
class LirBis(val op1: LirOperand, val op2: LirOperand) : LirInstruction()

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

class LirJmp(offset: Int): LirInstruction()
class LirJe(offset: Int): LirInstruction()
class LirJne(offset: Int): LirInstruction()
class LirJgt(offset: Int): LirInstruction()
class LirJlt(offset: Int): LirInstruction()
class LirJge(offset: Int): LirInstruction()
class LirJle(offset: Int): LirInstruction()
class LirJpl(offset: Int): LirInstruction()
class LirJmi(offset: Int): LirInstruction()

class LirJmpAbs(op: Operand): LirInstruction()

class LirCall(op: Operand): LirInstruction()
class LirRet(): LirInstruction()
class LirPush(op: Operand): LirInstruction()
class LirPop(op: Operand): LirInstruction()