package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.Program

abstract class Generator(val program: Program) {

    abstract fun addFunction(name: String, irOperations: List<Operation>)
    abstract fun generateAssemblyCode(): String

}