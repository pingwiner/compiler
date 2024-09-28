package org.pingwiner.compiler

enum class TokenType {
    NUMBER,
    SYMBOL,
    KEYWORD,
    OPERATOR,
    L_BRACE,
    R_BRACE,
    L_CURL,
    R_CURL,
    COMMA,
    END
}

enum class KeywordType(val value: String) {
    VAR("var"),
    RETURN("return"),
    FUN("fun")
}

enum class OperatorType {
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    EQUALS
}

open class Token(val tokenType: TokenType, val line: Int, val position: Int) {
    override fun toString(): String {
        return "Token($tokenType)"
    }
}

class Symbol(val content: String, line: Int, position: Int) : Token(TokenType.SYMBOL, line, position) {
    override fun toString(): String {
        return "Symbol($content)"
    }
}

class Number(val value: Int, line: Int, position: Int) : Token(TokenType.NUMBER, line, position) {
    override fun toString(): String {
        return "Number($value)"
    }
}

class Keyword(val type: KeywordType, line: Int, position: Int) : Token(TokenType.KEYWORD, line, position) {
    override fun toString(): String {
        return "Keyword($type)"
    }
}

class Operator(val type: OperatorType, line: Int, position: Int) : Token(TokenType.OPERATOR, line, position) {
    override fun toString(): String {
        return "Operator($type)"
    }
}

fun findToken(tokenType: TokenType, tokens: List<Token>, start: Int): Int {
    var i = start
    while (i < tokens.size) {
        if (tokens[i].tokenType == tokenType) return i
        i++
    }
    return -1
}
