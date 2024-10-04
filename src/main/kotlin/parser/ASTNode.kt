package org.pingwiner.compiler.parser

sealed class ASTNode {
    class Plus(val left: ASTNode, val right: ASTNode) : ASTNode()
    class Minus(val left: ASTNode, val right: ASTNode) : ASTNode()
    class Multiply(val left: ASTNode, val right: ASTNode) : ASTNode()
    class Divide(val left: ASTNode, val right: ASTNode) : ASTNode()
    class Assign(val left: ASTNode, val right: ASTNode) : ASTNode()
    class ImmediateValue(val value: Int) : ASTNode()
    class Variable(val name: String): ASTNode()
    class Result(): ASTNode()
    class FunctionCall(val name: String, val arguments: List<ASTNode>): ASTNode()
}
