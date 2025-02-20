package org.pingwiner.compiler.codegen.pdp11

class RegUsage {
    private val data = mutableMapOf<String, Pair<Int, Int>>()

    fun use(op: LirOperand, index: Int) {
        if (op.type == LirOperandType.Register) {
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

    companion object {
        fun create(operations: List<LirInstruction>): RegUsage {
            val usage = RegUsage()
            for ((i, op) in operations.withIndex()) {
                when (op) {
                    is DoubleOpInstruction -> {
                        usage.use(op.src, i)
                        usage.use(op.dst, i)
                    }
                    is SingleOpInstruction -> {
                        usage.use(op.op, i)
                    }
                    is LirJmpAbs -> {
                        usage.use(op.op, i)
                    }
                    is LirSob -> {
                        usage.use(op.reg, i)
                    }
                    is LirPush -> {
                        usage.use(op.op, i)
                    }
                    is LirPop -> {
                        usage.use(op.op, i)
                    }
                }
            }
            return usage
        }
    }
}

fun reduceRegUsage(instructions: List<LirInstruction>): Pair<Set<String>, Set<String>> {
    val regUsage = RegUsage.create(instructions)
    val regs = mutableMapOf<String, String>()
    val containsCalls = instructions.filterIsInstance<LirCall>().isNotEmpty()
    val localVars = mutableSetOf<String>()

    fun getRegFor(regVal: String): String {
        if (regVal == "_R0") return "_R0"
        for (reg in regs.keys) {
            if (regs[reg] == regVal) return reg
        }
        // If instructions contains calls, R0 will be reserved for returning value from subroutines
        // TODO: Fix it later and make R0 available for regular usage
        var regNum = if (containsCalls) 1 else 0
        while(true) {
            val regName = "R$regNum"
            if (!regs.containsKey(regName) || regs[regName]?.isEmpty() == true) {
                regs[regName] = regVal
                return regName
            }
            regNum++
        }
    }

    fun clearRegs(i: Int) {
        for (reg in regs.keys) {
            if (!regUsage.isRegUsed(regs[reg]!!, i)) {
                regs[reg] = ""
            }
        }
    }

    fun isTempReg(op: LirOperand): Boolean {
        if (op.type == LirOperandType.LocalVar) {
            if (op.name == "_R0") {
                // Register R0 will be allocated for _R0 variable so just return true here
                return true
            }
            localVars.add(op.name)
        }
        return op.usesRegister() && op.name.startsWith("%")
    }

    fun makeOperandFrom(op: LirOperand, newName: String): LirOperand {
        val availableRegNames = setOf("R0", "R1", "R2", "R3", "R4", "R5")
        // Reserve R0 for local variable _R0
        if ((op.type == LirOperandType.LocalVar) && (newName == "_R0")) {
            return LirOperand(LirOperandType.Register, "R0", 0)
        }
        return if (availableRegNames.contains(newName)) {
            op.copy(
                name = newName
            )
        } else {
            localVars.add("_$newName")
            op.copy(
                name = "_$newName",
                type = LirOperandType.LocalVar
            )
        }
    }

    for ((i, instruction) in instructions.withIndex()) {
        clearRegs(i)
        when (instruction) {
            is DoubleOpInstruction -> {
                if (isTempReg(instruction.src)) {
                    val newSrc = getRegFor(instruction.src.name)
                    instruction.src = makeOperandFrom(instruction.src, newSrc)
                }
                if (isTempReg(instruction.dst)) {
                    val newDst = getRegFor(instruction.dst.name)
                    instruction.dst = makeOperandFrom(instruction.dst, newDst)
                }
            }
            is SingleOpInstruction -> {
                if (isTempReg(instruction.op)) {
                    val newOp = getRegFor(instruction.op.name)
                    instruction.op = makeOperandFrom(instruction.op, newOp)
                }
            }
            is LirJmpAbs -> {
                if (isTempReg(instruction.op)) {
                    val newOp = getRegFor(instruction.op.name)
                    instruction.op = makeOperandFrom(instruction.op, newOp)
                }
            }
            is LirSob -> {
                if (isTempReg(instruction.reg)) {
                    val newReg = getRegFor(instruction.reg.name)
                    instruction.reg = makeOperandFrom(instruction.reg, newReg)
                }
            }
            is LirPush -> {
                if (isTempReg(instruction.op)) {
                    val newOp = getRegFor(instruction.op.name)
                    instruction.op = makeOperandFrom(instruction.op, newOp)
                }
            }
            is LirPop -> {
                if (isTempReg(instruction.op)) {
                    val newOp = getRegFor(instruction.op.name)
                    instruction.op = makeOperandFrom(instruction.op, newOp)
                }
            }

        }
    }
    return Pair(regs.keys, localVars)
}

fun printRegUsage(instructions: List<LirInstruction>) {
    val usage = RegUsage.create(instructions)
    for (i in instructions.indices) {
        val regs = usage.getUsedRegs(i)
        val sb = StringBuilder()
        for (r in regs) {
            sb.append(r)
            if (r != regs.last()) sb.append(", ")
        }
        println(sb.toString())
    }
}
