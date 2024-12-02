package org.pingwiner.compiler

import org.pingwiner.compiler.Operator.Minus

enum class State {
    SPACE,
    NUMBER,
    SYMBOL,
    OPERATOR,
    COMMENT
}

const val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
const val digits = "0123456789"
const val hexDigits = "0123456789abcdefABCDEF"
const val ops = "+-*/=<>?:&|%^!"

fun Char.isLetter(): Boolean {
    return letters.contains(this)
}

fun Char.isDigit(): Boolean {
    return digits.contains(this)
}

fun Char.isHexDigit(): Boolean {
    return hexDigits.contains(this)
}

fun Char.isOp(): Boolean {
    return ops.contains(this)
}

class Lexer {
    val tokens = mutableListOf<Token>()
    private var state: State = State.SPACE
    private var currentToken = StringBuilder()
    private var hexMode = false
    var line = 1
    var position = 1

    fun reset() {
        tokens.clear()
        state = State.SPACE
        currentToken = StringBuilder()
        hexMode = false
        line = 1
        position = 1
    }

    fun scan(input: String) {
        for (c in "$input ") {
            when(state) {
                State.SPACE -> space(c)
                State.NUMBER -> number(c)
                State.SYMBOL -> word(c)
                State.OPERATOR -> operator(c)
                State.COMMENT -> {}
            }
            if (c == '\n') {
                position = 1
                line++
                if (state == State.COMMENT) {
                    state = State.SPACE
                }
            } else {
                position++
            }
        }
    }

    private fun space(c: Char) {
        if (c.isLetter()) {
            currentToken.clear()
            currentToken.append(c)
            state = State.SYMBOL
        } else if (c.isDigit()) {
            currentToken.clear()
            currentToken.append(c)
            hexMode = false
            state = State.NUMBER
        } else if (c.isOp()) {
            currentToken.clear()
            currentToken.append(c)
            state = State.OPERATOR
        } else {
            //Try to parse known special symbols
            val specialSymbol = SpecialSymbol.parse(c, line, position)
            if (specialSymbol != null) {
                tokens.add(specialSymbol)
            } else if (c == '#') {
                state = State.COMMENT
            } else {
                //Some unrecognized character. Just skip it.
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun number(c: Char) {
        if (!hexMode && c.isDigit()) {
            currentToken.append(c)
        } else if (hexMode && c.isHexDigit()) {
            currentToken.append(c)
        } else {
            if (!hexMode && (c == 'x') && (currentToken.length == 1) && (currentToken[0] == '0')) {
                hexMode = true
                currentToken.append(c)
                return
            }
            val format = HexFormat { number.prefix = "0x" }
            var intValue = if (hexMode) currentToken.toString().hexToInt(format) else currentToken.toString().toInt()

            if (isUnaryMinus()) {
                intValue = -intValue
                tokens.remove(tokens.last())
            }

            tokens.add(Number(intValue, line, position - currentToken.length))
            currentToken.clear()
            state = State.SPACE
            space(c)
        }
    }

    private fun isUnaryMinus(): Boolean {
        if (tokens.isEmpty()) return false
        if (tokens.size < 2) return false
        val lastToken = tokens.last()
        val prevLastToken = tokens[tokens.size - 2]
        return (lastToken is Minus) && (
                (prevLastToken is Operator) ||
                (prevLastToken is SpecialSymbol.LBrace) ||
                (prevLastToken is SpecialSymbol.LSquare) ||
                (prevLastToken is SpecialSymbol.LCurl) ||
                (prevLastToken is SpecialSymbol.Comma))
    }

    private fun word(c: Char) {
        if (c.isLetter() || c.isDigit()) {
            currentToken.append(c)
        } else {
            val s = currentToken.toString()
            val operator = Operator.parse(s, line, position)
            val keyword = Keyword.parse(s, line, position)
            if (operator != null) {
                tokens.add(operator)
            } else if (keyword != null) {
                tokens.add(keyword)
                if (keyword is Keyword.Return) {
                    tokens.add(Operator.Assign(line, position))
                }
            } else {
                tokens.add(Symbol(s, line, position - s.length))
            }
            currentToken.clear()
            state = State.SPACE
            space(c)
        }
    }

    private fun operator(c: Char) {
        if (c.isOp()) {
            currentToken.append(c)
        } else {
            val s = currentToken.toString()
            val operator = Operator.parse(s, line, position)
            if (operator != null) {
                tokens.add(operator)
            } else {
                throw IllegalArgumentException("Syntax error at line $line, position $position")
            }
            currentToken.clear()
            state = State.SPACE
            space(c)
        }
    }

    fun printTokens() {
        for (token in tokens) {
            println(token)
        }
    }

}