package org.pingwiner.compiler.codegen.pdp11

class RegUsage {
    private val data = mutableMapOf<String, Pair<Int, Int>>()

    fun use(op: LirOperand, index: Int) {
        if (op.type == LirOperandType.register) {
            if (op.name.startsWith("%")) {
                if (!data.containsKey(op.name)) {
                    data[op.name] = Pair(index, index)
                } else {
                    val oldVal = data[op.name]
                    data[op.name] = oldVal!!.copy(
                        second = index
                    )
                }
            }
        }
    }

    fun isRegUsed(name: String, index: Int): Boolean {
        val v = data[name] ?: return false
        return (v.first <= index) and (v.second >= index)
    }

    fun getUsedRegs(index: Int): Set<String> {
        val result = mutableSetOf<String>()
        val regs = data.keys
        for (reg in regs) {
            if (isRegUsed(reg, index)) {
                result.add(reg)
            }
        }
        return result
    }
}
