package org.pingwiner.compiler.codegen.pdp11

import java.util.*

fun oct(arg: String): Int {
    val octalDigits = listOf('0', '1', '2', '3', '4', '5', '6', '7')

    if (arg.isBlank()) return 0
    val pos = arg.length - 1
    var result = 0
    for (c in arg) {
        if (!octalDigits.contains(c)) throw IllegalArgumentException("$c is not an octal digit")
        val code = octalDigits.indexOf(c)
        result += code shl (3 * pos)
    }
    return result
}

fun oct(arg: Int): Int {
    return oct(arg.toString())
}

fun Int.asOctal(): String {
    return Integer.toOctalString(this)
}