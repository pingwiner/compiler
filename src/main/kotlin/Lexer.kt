package org.pingwiner.compiler

enum class State {
    SPACE,
    NUMBER,
    VAR_NAME,
    OPERATOR
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

    fun scan(input: String) {
        for (c in input) {
            when(state) {
                State.SPACE -> space(c)
                State.NUMBER -> number(c)
                State.VAR_NAME -> word(c)
                State.OPERATOR -> operator(c)
            }
            if (c == '\n') {
                position = 1
                line++
            } else {
                position++
            }
        }
    }

    private fun space(c: Char) {
        val specialSymbols = mapOf(
            '(' to TokenType.L_BRACE,
            ')' to TokenType.R_BRACE,
            '{' to TokenType.L_CURL,
            '}' to TokenType.R_CURL,
            '[' to TokenType.L_SQUARE,
            ']' to TokenType.R_SQUARE,
            ';' to TokenType.END,
            ',' to TokenType.COMMA
        )

        if (c.isLetter()) {
            currentToken.clear()
            currentToken.append(c)
            state = State.VAR_NAME
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
            specialSymbols[c]?.let { tokenType ->
                tokens.add(Token(tokenType, line, position))
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
            val intValue = if (hexMode) currentToken.toString().hexToInt(format) else currentToken.toString().toInt()
            tokens.add(Number(intValue, line, position - currentToken.length))
            currentToken.clear()
            state = State.SPACE
            space(c)
        }
    }

    private fun word(c: Char) {
        if (c.isLetter() || c.isDigit()) {
            currentToken.append(c)
        } else {
            val s = currentToken.toString()
            val operator = parseOperator(s)
            val keyword = parseKeyword(s)
            if (operator != null) {
                tokens.add(operator)
            } else if (keyword != null) {
                tokens.add(keyword)
                if (keyword.type == KeywordType.RETURN) {
                    tokens.add(Operator(OperatorType.ASSIGN, line, position))
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
            val operator = parseOperator(s)
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

    private fun parseKeyword(s: String) : Keyword? {
        for (k in KeywordType.entries) {
            if (s == k.value) return Keyword(k, line, position - k.value.length)
        }
        return null
    }

    private fun parseOperator(s: String) : Operator? {
        for (k in OperatorType.entries) {
            if (s == k.value) return Operator(k, line, position - k.value.length)
        }
        return null
    }

}