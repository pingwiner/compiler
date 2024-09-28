package org.pingwiner.compiler

enum class State {
    SPACE,
    NUMBER,
    VAR_NAME
}

const val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
const val digits = "0123456789"
const val ops = "+-*/="

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
            tokens.add(createOperator(c))
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

    private fun createOperator(c: Char): Token {
        return when(c){
            '+' -> Operator(OperatorType.PLUS, line, position)
            '-' -> Operator(OperatorType.MINUS, line, position)
            '*' -> Operator(OperatorType.MULTIPLY, line, position)
            '/' -> Operator(OperatorType.DIVIDE, line, position)
            else -> Operator(OperatorType.EQUALS, line, position)
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

}