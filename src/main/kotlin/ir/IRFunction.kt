package org.pingwiner.compiler.codegen

data class IRFunction(
    val name: String,
    val arguments: List<String>,
    val localVars: List<String>,
    val operations: List<Operation>
)
