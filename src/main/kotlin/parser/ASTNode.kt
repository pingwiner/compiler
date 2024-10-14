package org.pingwiner.compiler.parser

sealed class ASTNode(val left: ASTNode? = null, val right: ASTNode? = null, private val operation: String = "") {

    override fun toString(): String {
        return "$left $operation $right"
    }

    class Plus(left: ASTNode, right: ASTNode) : ASTNode(left, right, "+")

    class Minus(left: ASTNode, right: ASTNode) : ASTNode(left, right, "-")

    class Multiply(left: ASTNode, right: ASTNode) : ASTNode(left, right, "*")

    class Divide(left: ASTNode, right: ASTNode) : ASTNode(left, right, "/")

    class Assign(left: ASTNode, right: ASTNode) : ASTNode(left, right, "=")

    class If(left: ASTNode, right: ASTNode) : ASTNode(left, right, "?")

    class Else(left: ASTNode, right: ASTNode) : ASTNode(left, right, ":")

    class Eq(left: ASTNode, right: ASTNode) : ASTNode(left, right, "==") {
        override fun toString(): String {
            return "($left == $right)"
        }
    }

    class Lt(left: ASTNode, right: ASTNode) : ASTNode(left, right, "<")

    class Gt(left: ASTNode, right: ASTNode) : ASTNode(left, right, ">")

    class GtEq(left: ASTNode, right: ASTNode) : ASTNode(left, right, ">=")

    class LtEq(left: ASTNode, right: ASTNode) : ASTNode(left, right, "<=")

    class ImmediateValue(val value: Int) : ASTNode() {
        override fun toString(): String {
            return "$value"
        }
    }

    class Variable(val name: String, val index: ASTNode? = null): ASTNode() {
        override fun toString(): String {
            return name
        }
    }

    class Result: ASTNode() {
        override fun toString(): String {
            return "result"
        }
    }

    class FunctionCall(val name: String, val arguments: List<ASTNode>): ASTNode() {
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

    class While(left: ASTNode, right: ASTNode) : ASTNode(left, right) {
        override fun toString(): String {
            return "$left while($right)"
        }
    }

    class Repeat(left: ASTNode, right: ASTNode) : ASTNode(left, right) {
        override fun toString(): String {
            return "$left repeat($right)"
        }
    }

    class Until(left: ASTNode, right: ASTNode) : ASTNode(left, right) {
        override fun toString(): String {
            return "$left until($right)"
        }
    }

    class Neq(left: ASTNode, right: ASTNode) : ASTNode(left, right, "!=")

    class Shr(left: ASTNode, right: ASTNode) : ASTNode(left, right, ">>")

    class Shl(left: ASTNode, right: ASTNode) : ASTNode(left, right, "<<")

    class OrB(left: ASTNode, right: ASTNode) : ASTNode(left, right, "|")

    class OrL(left: ASTNode, right: ASTNode) : ASTNode(left, right, "||")

    class AndB(left: ASTNode, right: ASTNode) : ASTNode(left, right, "&")

    class AndL(left: ASTNode, right: ASTNode) : ASTNode(left, right, "&&")

    class Xor(left: ASTNode, right: ASTNode) : ASTNode(left, right, "^")

    class Mod(left: ASTNode, right: ASTNode) : ASTNode(left, right, "%")

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
}
