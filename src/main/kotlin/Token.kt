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
    RESULT("result"),
    FUN("fun"),
    VAR("var")
}

enum class OperatorType(val value: String) {
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    ASSIGN("="),
    IF("?"),
    ELSE(":"),
    EQ("=="),
    LT("<"),
    GT(">"),
    GTEQ(">="),
    LTEQ("<=")
}

open class Token(val tokenType: TokenType, val line: Int, val position: Int) {
    override fun toString(): String {
        return "Token($tokenType)"
    }

    fun at(offset: Int = 0): String {
        return "at line $line, position ${position + offset}"
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

