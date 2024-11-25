package org.pingwiner.compiler.codegen

import org.pingwiner.compiler.parser.Program

abstract class Generator(val program: Program) {

    abstract fun generate(operations: List<Operation>): ByteArray

}