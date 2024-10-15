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
    L_SQUARE,
    R_SQUARE,
    COMMA,
    END
}

enum class KeywordType(val value: String) {
    RETURN("return"),
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
    NEQ("!="),
    LT("<"),
    GT(">"),
    GTEQ(">="),
    LTEQ("<="),
    SHR(">>"),
    SHL("<<"),
    ORB("|"),
    ORL("||"),
    ANDB("&"),
    ANDL("&&"),
    XOR("^"),
    MOD("%"),
    WHILE("while"),
    REPEAT("repeat"),
    UNTIL("until")
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

