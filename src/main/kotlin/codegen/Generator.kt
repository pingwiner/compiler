package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.ASTNode

class Generator {
    val operations = mutableListOf<Operation>()

    private fun processNode(node: ASTNode) {
        when(node) {
            is ASTNode.BinaryOperation -> {
                processBinaryOperation(node)
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
            is ASTNode.ImmediateValue -> {}
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
            else -> { }
        }
    }

    private fun processBinaryOperation(node: ASTNode.BinaryOperation) {
        when (node) {
            is ASTNode.BinaryOperation.Plus -> {

            }
            else -> {}
        }
    }
}