package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.ASTNode

class Generator {

    companion object {
        var regCount = 0
    }

    val operations = mutableListOf<Operation>()

    fun processNode(node: ASTNode): String {
        when(node) {
            is ASTNode.BinaryOperation -> {
                return processBinaryOperation(node)
            }
            is ASTNode.Block -> {
                for (subNode in node.subNodes) {
                    processNode(subNode)
                }
            }
            is ASTNode.FunctionCall -> {
                for (arg in node.arguments) {
                    processNode(arg)
                }
                //generate function call
            }
            is ASTNode.ImmediateValue -> { return node.toString() }
            is ASTNode.Variable -> { return node.toString() }
            is ASTNode.Inv -> {}
            is ASTNode.Neg -> {}
            is ASTNode.Repeat -> {
                processNode(node.statement)
                processNode(node.count)
            }
            is ASTNode.Return -> {
                node.value?.let {
                    processNode(it)
                }
            }
            is ASTNode.Until -> {
                processNode(node.statement)
                processNode(node.condition)
            }
            is ASTNode.While -> {
                processNode(node.statement)
                processNode(node.condition)
            }
            else -> ""
        }
        return ""
    }

    private fun processBinaryOperation(node: ASTNode.BinaryOperation): String {
        if (node is ASTNode.BinaryOperation.Assign) return ""
        val reg = "R${regCount++}"

        if (node.isFinal()) {
            return this.toString()
        } else {
            val leftVal = processNode(node.left)
            val rightVal = processNode(node.right)
            println("$reg = $leftVal ${node.operation} $rightVal")
            return reg
        }
    }
}