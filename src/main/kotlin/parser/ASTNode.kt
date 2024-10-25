package org.pingwiner.compiler.parser

sealed class ASTNode {

    enum class Operation(val op: String) {
        PLUS("+"),
        MINUS("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        ASSIGN("="),
        IF("?"),
        ELSE(":"),
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
        MOD("%");

        override fun toString(): String {
            return op
        }
    }

    open fun isFinal(): Boolean = false

    sealed class BinaryOperation(val left: ASTNode, val right: ASTNode, val operation: Operation) : ASTNode() {
        override fun toString(): String {
            return "$left $operation $right"
        }

        fun create(left: ASTNode, right: ASTNode): BinaryOperation {
            return when(operation) {
                Operation.PLUS -> Plus(left, right)
                Operation.MINUS -> Minus(left, right)
                Operation.MULTIPLY -> Multiply(left, right)
                Operation.DIVIDE -> Divide(left, right)
                Operation.ASSIGN -> Assign(left, right)
                Operation.IF -> If(left, right)
                Operation.ELSE -> Else(left, right)
                Operation.EQ -> Eq(left, right)
                Operation.LT -> Lt(left, right)
                Operation.GT -> Gt(left, right)
                Operation.GTEQ -> GtEq(left, right)
                Operation.LTEQ -> LtEq(left, right)
                Operation.NEQ -> Neq(left, right)
                Operation.SHR -> Shr(left, right)
                Operation.SHL -> Shl(left, right)
                Operation.OR -> Or(left, right)
                Operation.AND -> And(left, right)
                Operation.XOR -> Xor(left, right)
                Operation.MOD -> Mod(left, right)
            }
        }

        class Plus(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.PLUS)
        class Minus(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.MINUS)
        class Multiply(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.MULTIPLY)
        class Divide(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.DIVIDE)
        class Assign(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.ASSIGN)
        class If(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.IF)
        class Else(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.ELSE)
        class Eq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.EQ) {
            override fun toString(): String {
                return "($left == $right)"
            }
        }
        class Lt(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.LT)
        class Gt(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.GT)
        class GtEq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.GTEQ)
        class LtEq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.LTEQ)
        class Neq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.NEQ)
        class Shr(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.SHR)
        class Shl(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.SHL)
        class Or(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.OR)
        class And(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.AND)
        class Xor(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.XOR)
        class Mod(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, Operation.MOD)
    }

    class ImmediateValue(val value: Int) : ASTNode() {
        override fun toString(): String {
            return "$value"
        }

        override fun isFinal(): Boolean = true

    }

    class Variable(val name: String, val index: ASTNode? = null) : ASTNode() {
        override fun toString(): String {
            if (index != null) {
                return "$name[$index]"
            } else {
                return name
            }
        }

        override fun isFinal(): Boolean = true
    }

    class Return(val value: ASTNode? = null) : ASTNode() {
        override fun toString(): String {
            return "return $value"
        }
    }

    class FunctionCall(val name: String, val arguments: List<ASTNode>) : ASTNode() {
        override fun toString(): String {
            val sb = StringBuilder()
            var i = 0
            for (arg in arguments) {
                sb.append(arg.toString())
                i += 1
                if (i < arguments.size) {
                    sb.append(", ")
                }
            }
            return "$name($sb)"
        }
    }

    class While(val statement: ASTNode, val condition: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$statement while($condition)"
        }
    }

    class Repeat(val statement: ASTNode, val count: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$statement repeat($count)"
        }
    }

    class Until(val statement: ASTNode, val condition: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$statement until($condition)"
        }
    }


    class Neg(val arg: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "-($arg)"
        }
    }

    class Inv(val arg: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "~($arg)"
        }
    }

    class Block(val subNodes: List<ASTNode>) : ASTNode()

}