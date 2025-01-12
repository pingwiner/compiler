package org.pingwiner.compiler.codegen.pdp11

data class LirFunction(
    val name: String,
    var instructions: List<LirInstruction>,
    val usedRegisters: Set<String>,
    val localVars: List<String>
) {
    fun getLocalVarOffset(name: String): Int {
        if (!localVars.contains(name)) {
            throw IllegalArgumentException("Variable $name does not exist")
        }
        val size = localVars.size
        val index = localVars.indexOf(name)
        return (size - index - 1) * 2
    }
}
