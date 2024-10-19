package org.pingwiner.compiler.parser

sealed class ASTNode {

    sealed class BinaryOperation(val left: ASTNode, val right: ASTNode, private val operation: String) : ASTNode() {
        override fun toString(): String {
            return "$left $operation $right"
        }

        class Plus(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "+")

        class Minus(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "-")

        class Multiply(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "*")

        class Divide(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "/")

        class Assign(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "=")

        class If(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "?")

        class Else(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, ":")

        class Eq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "==") {
            override fun toString(): String {
                return "($left == $right)"
            }
        }

        class Lt(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "<")

        class Gt(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, ">")

        class GtEq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, ">=")

        class LtEq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "<=")

        class Neq(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "!=")

        class Shr(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, ">>")

        class Shl(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "<<")

        class Or(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "|")

        class And(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "&")

        class Xor(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "^")

        class Mod(left: ASTNode, right: ASTNode) : BinaryOperation(left, right, "%")
    }

    class ImmediateValue(val value: Int) : ASTNode() {
        override fun toString(): String {
            return "$value"
        }
    }

    class Variable(val name: String, val index: ASTNode? = null) : ASTNode() {
        override fun toString(): String {
            if (index != null) {
                return "$name[$index]"
            } else {
                return name
            }
        }
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