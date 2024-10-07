package org.pingwiner.compiler.parser

sealed class ASTNode {
    class Plus(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left + $right"
        }
    }
    class Minus(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left - $right"
        }
    }
    class Multiply(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left * $right"
        }
    }
    class Divide(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left / $right"
        }
    }
    class Assign(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left = $right"
        }
    }
    class If(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left ? $right"
        }
    }
    class Else(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left : $right"
        }
    }
    class Eq(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "($left == $right)"
        }
    }
    class Lt(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left < $right"
        }
    }
    class Gt(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left > $right"
        }
    }
    class GtEq(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left >= $right"
        }
    }
    class LtEq(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left <= $right"
        }
    }
    class ImmediateValue(val value: Int) : ASTNode() {
        override fun toString(): String {
            return "$value"
        }
    }
    class Variable(val name: String): ASTNode() {
        override fun toString(): String {
            return name
        }
    }
    class Result(): ASTNode() {
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
    class While(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left while($right)"
        }
    }
    class Repeat(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left repeat($right)"
        }
    }
    class Until(val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "$left until($right)"
        }
    }
}
