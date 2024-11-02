package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.ASTNode

enum class OperandType {
    ImmediateValue,
    Register,
    LocalVariable,
    GlobalVariable,
    Label
}

data class Operand(
    val name: String,
    val type: OperandType,
    val value: Int? = null
) {
    fun isZero(): Boolean {
        return type == OperandType.ImmediateValue && value == 0
    }

    override fun toString(): String {
        if (type == OperandType.ImmediateValue) {
            return "$value"
        } else {
            return name
        }
    }
}

enum class Operator(val op: String) {
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    EQ("=="),
    LT("<"),
    GT(">"),
    GTEQ(">="),
    LTEQ("<="),
    NEQ("!="),
    SHR(">>"),
    SHL("<<"),
    OR("|"),
    AND("&"),
    XOR("^"),
    MOD("%"),
    IF("?");

    companion object {
        fun from(node: ASTNode.BinaryOperation): Operator {
            return when(node) {
                is ASTNode.BinaryOperation.Plus -> PLUS
                is ASTNode.BinaryOperation.And -> AND
                is ASTNode.BinaryOperation.Assign -> TODO()
                is ASTNode.BinaryOperation.Divide -> DIVIDE
                is ASTNode.BinaryOperation.Else -> TODO()
                is ASTNode.BinaryOperation.Eq -> EQ
                is ASTNode.BinaryOperation.Gt -> GT
                is ASTNode.BinaryOperation.GtEq -> GTEQ
                is ASTNode.BinaryOperation.If -> IF
                is ASTNode.BinaryOperation.Lt -> LT
                is ASTNode.BinaryOperation.LtEq -> LTEQ
                is ASTNode.BinaryOperation.Minus -> MINUS
                is ASTNode.BinaryOperation.Mod -> MOD
                is ASTNode.BinaryOperation.Multiply -> MULTIPLY
                is ASTNode.BinaryOperation.Neq -> NEQ
                is ASTNode.BinaryOperation.Or -> OR
                is ASTNode.BinaryOperation.Shl -> SHL
                is ASTNode.BinaryOperation.Shr -> SHR
                is ASTNode.BinaryOperation.Xor -> XOR
            }
        }
    }
}

sealed class Operation(val result: Operand) {
    class BinaryOperation(result: Operand, val operand1: Operand, val operand2: Operand, val operator: Operator) : Operation(result) {
        override fun toString(): String {
            return result.name + " = " + operand1 + " " + operator.op + " " + operand2
        }
    }
    class Assignment(result: Operand, val operand: Operand): Operation(result) {
        override fun toString(): String {
            return result.name + " = " + operand
        }
    }
    class Return(result: Operand) : Operation(result) {
        override fun toString(): String {
            return "return " + result.name
        }
    }
    class IfNot(val condition: Operand, label: Operand) : Operation(label) {
        override fun toString(): String {
            return "IfNot(${condition.name}) goto " + result.name
        }
    }
    class Label(name: String) : Operation(Operand(name, OperandType.ImmediateValue)) {
        override fun toString(): String {
            return result.name + ":"
        }
    }
}