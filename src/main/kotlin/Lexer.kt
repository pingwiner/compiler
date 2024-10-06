package org.pingwiner.compiler

enum class State {
    SPACE,
    NUMBER,
    VAR_NAME,
    OPERATOR
}

const val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
const val digits = "0123456789"
const val ops = "+-*/=<>?:"

fun Char.isLetter(): Boolean {
    return letters.contains(this)
}

fun Char.isDigit(): Boolean {
    return digits.contains(this)
}

fun Char.isOp(): Boolean {
    return ops.contains(this)
}

class Lexer {
    val tokens = mutableListOf<Token>()
    private var state: State = State.SPACE
    private var currentToken = StringBuilder()
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
        if (c.isLetter()) {
            currentToken.clear()
            currentToken.append(c)
            state = State.VAR_NAME
        } else if (c.isDigit()) {
            currentToken.clear()
            currentToken.append(c)
            state = State.NUMBER
        } else if (c.isOp()) {
            currentToken.clear()
            currentToken.append(c)
            state = State.OPERATOR
        } else if (c == '(') {
            tokens.add(Token(TokenType.L_BRACE, line, position))
        } else if (c == ')') {
            tokens.add(Token(TokenType.R_BRACE, line, position))
        } else if (c == ';') {
            tokens.add(Token(TokenType.END, line, position))
        } else if (c == '{') {
            tokens.add(Token(TokenType.L_CURL, line, position))
        } else if (c == '}') {
            tokens.add(Token(TokenType.R_CURL, line, position))
        } else if (c == ',') {
            tokens.add(Token(TokenType.COMMA, line, position))
        }
    }

    private fun number(c: Char) {
        if (c.isDigit()) {
            currentToken.append(c)
        } else {
            tokens.add(Number(currentToken.toString().toInt(), line, position - currentToken.length))
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
            val keyword = parseKeyword(s)
            if (keyword != null) {
                tokens.add(keyword)
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