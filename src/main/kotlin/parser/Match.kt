package org.pingwiner.compiler.parser

import org.pingwiner.compiler.Token

class Match(val tokens: List<Token>, var next: Int) {
    var valid = true
    val values = mutableListOf<Any>()

    inline fun <reified T : Token> expect(): Match {
        if (tokens.size <= next) {
            valid = false
            return this
        }
        if (tokens[next] is T) {
            values.add(tokens[next] as T)
        } else {
            valid = false
        }
        next++
        return this
    }

    fun <T>get(i: Int): T {
        return values[i] as T
    }
}
