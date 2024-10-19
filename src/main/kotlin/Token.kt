package org.pingwiner.compiler

abstract class Token(val line: Int, val position: Int) {
    fun at(offset: Int = 0): String {
        return "at line $line, position ${position + offset}"
    }
}

class Symbol(val content: String, line: Int, position: Int) : Token(line, position) {
    override fun toString(): String {
        return "Symbol($content)"
    }
}

class Number(val value: Int, line: Int, position: Int) : Token(line, position) {
    override fun toString(): String {
        return "Number($value)"
    }
}

sealed class Keyword(line: Int, position: Int) : Token(line, position) {
    override fun toString(): String {
        return this.javaClass.simpleName
    }

    companion object {
        fun parse(s: String, line: Int, position: Int) : Keyword? {
            return when (s) {
                "return" -> Return(line, position)
                "fun" -> Fun(line, position)
                "var" -> Var(line, position)
                else -> null
            }
        }
    }

    class Return(line: Int, position: Int) : Keyword(line, position)
    class Fun(line: Int, position: Int) : Keyword(line, position)
    class Var(line: Int, position: Int) : Keyword(line, position)
}


sealed class Operator(line: Int, position: Int) : Token(line, position) {
    override fun toString(): String {
        return "Operator(${this.javaClass.simpleName})"
    }

    companion object {
        fun parse(s: String, line: Int, position: Int): Operator? {
            return when(s) {
                "+" -> Plus(line, position)
                "-" -> Minus(line, position)
                "*" -> Multiply(line, position)
                "/" -> Divide(line, position)
                "=" -> Assign(line, position)
                "?" -> If(line, position)
                ":" -> Else(line, position)
                "==" -> Eq(line, position)
                "!=" -> Neq(line, position)
                "<" -> Lt(line, position)
                ">" -> Gt(line, position)
                ">=" -> GtEq(line, position)
                "<=" -> LtEq(line, position)
                ">>" -> Shr(line, position)
                "<<" -> Shl(line, position)
                "|" -> Or(line, position)
                "&" -> And(line, position)
                "^" -> Xor(line, position)
                "%" -> Mod(line, position)
                "while" -> While(line, position)
                "repeat" -> Repeat(line, position)
                "until" -> Until(line, position)
                else -> null
            }
        }
    }

    class Plus(line: Int, position: Int) : Operator(line, position)
    class Minus(line: Int, position: Int) : Operator(line, position)
    class Multiply(line: Int, position: Int) : Operator(line, position)
    class Divide(line: Int, position: Int) : Operator(line, position)
    class Assign(line: Int, position: Int) : Operator(line, position)
    class If(line: Int, position: Int) : Operator(line, position)
    class Else(line: Int, position: Int) : Operator(line, position)
    class Eq(line: Int, position: Int) : Operator(line, position)
    class Neq(line: Int, position: Int) : Operator(line, position)
    class Lt(line: Int, position: Int) : Operator(line, position)
    class Gt(line: Int, position: Int) : Operator(line, position)
    class GtEq(line: Int, position: Int) : Operator(line, position)
    class LtEq(line: Int, position: Int) : Operator(line, position)
    class Shr(line: Int, position: Int) : Operator(line, position)
    class Shl(line: Int, position: Int) : Operator(line, position)
    class Or(line: Int, position: Int) : Operator(line, position)
    class And(line: Int, position: Int) : Operator(line, position)
    class Xor(line: Int, position: Int) : Operator(line, position)
    class Mod(line: Int, position: Int) : Operator(line, position)
    class While(line: Int, position: Int) : Operator(line, position)
    class Repeat(line: Int, position: Int) : Operator(line, position)
    class Until(line: Int, position: Int) : Operator(line, position)
}

sealed class SpecialSymbol(line: Int, position: Int) : Token(line, position) {
    override fun toString(): String {
        return this::class.simpleName.toString()
    }

    companion object {
        fun parse(c: Char, line: Int, position: Int): SpecialSymbol? {
            return when (c) {
                '(' -> LBrace(line, position)
                ')' -> RBrace(line, position)
                '{' -> LCurl(line, position)
                '}' -> RCurl(line, position)
                '[' -> LSquare(line, position)
                ']' -> RSquare(line, position)
                ';' -> End(line, position)
                ',' -> Comma(line, position)
                else -> null
            }
        }
    }

    class LBrace(line: Int, position: Int) : SpecialSymbol(line, position)
    class RBrace(line: Int, position: Int) : SpecialSymbol(line, position)
    class LCurl(line: Int, position: Int) : SpecialSymbol(line, position)
    class RCurl(line: Int, position: Int) : SpecialSymbol(line, position)
    class LSquare(line: Int, position: Int) : SpecialSymbol(line, position)
    class RSquare(line: Int, position: Int) : SpecialSymbol(line, position)
    class End(line: Int, position: Int) : SpecialSymbol(line, position)
    class Comma(line: Int, position: Int) : SpecialSymbol(line, position)
}
