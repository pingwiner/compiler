package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.Program

abstract class Generator(val program: Program) {

    abstract fun addFunction(name: String, operations: List<Operation>)
    abstract fun generateAssemblyCode(): String

}