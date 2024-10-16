package org.pingwiner.compiler.codegen

sealed class Operand {
    class Immediate(value: Int) : Operand()
    class Memory(address: Int) : Operand()
}

sealed class Operation {
    class Ld(arg: Operand) : Operation()
    class Add(arg: Operand) : Operation()
    class Sub(arg: Operand) : Operation()
    class And(arg: Operand) : Operation()
    class Or(arg: Operand) : Operation()
    class Xor(arg: Operand) : Operation()
    data object Neg : Operation()
    data object Cpl : Operation()
    class Call(arg: Operand.Memory)
    data object Ret : Operation()
    class Inc(arg: Operand) : Operation()
    class Dec(arg: Operand) : Operation()
}