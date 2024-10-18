package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.Number

class Program : ParserContext {
    val functions = mutableListOf<Function>()
    val globalVars = mutableMapOf<String, Variable>()
    val useFunc = mutableMapOf<String, Int>()

    override fun useFunction(name: String) {
        if (useFunc.contains(name)) {
            useFunc[name] = useFunc[name]!!.plus(1)
        } else {
            useFunc[name] = 1
        }
    }

    override fun hasVariable(name: String): Boolean {
        return globalVars.containsKey(name)
    }

    override fun hasArray(name: String): Boolean {
        return globalVars[name]?.isArray() == true
    }

    override fun arraySize(name: String): Int {
        if (!hasArray(name)) return 0
        return globalVars[name]?.size ?: 0
    }

    fun parse(tokens: List<Token>) {
        var i = 0
        while(i < tokens.size) {
            val token = tokens[i]
            when(token) {
                is Keyword -> {
                    when(token.type) {
                        KeywordType.FUN -> {
                            i = parseFunction(tokens, i)
                        }
                        KeywordType.VAR -> {
                            i = parseVar(tokens, i)
                        }
                        else -> {
                            unexpectedTokenError(token)
                        }
                    }
                }
                else -> {
                    if (token.tokenType != TokenType.END) {
                        unexpectedTokenError(token)
                    } else {
                        i++
                    }
                }
            }
        }
        for (f in useFunc.keys) {
            var found = false
            for (function in functions) {
                if (function.name == f) {
                    found = true
                    break
                }
            }
            if (!found) {
                throw IllegalArgumentException("Function $f not defined")
            }
        }
    }

    private fun parseFunction(tokens: List<Token>, start: Int): Int {
        if ((tokens.size < start + 2) || (tokens[start + 1] !is Symbol)) {
            throw IllegalArgumentException("Function name expected " + tokens[start].at(4))
        }
        val functionName = (tokens[start + 1] as Symbol).content

        if (tokens[start + 2].tokenType != TokenType.L_BRACE) {
            throw IllegalArgumentException("Function parameters expected " + tokens[start + 2].at())
        }

        var i = start + 3
        val argNames = mutableListOf<String>()
        while(tokens[i].tokenType != TokenType.R_BRACE) {
            when(tokens[i].tokenType) {
                TokenType.SYMBOL -> {
                    argNames.add((tokens[i] as Symbol).content)
                }
                TokenType.COMMA -> {}
                else -> {
                    unexpectedTokenError(tokens[i])
                }
            }
            i++
        }

        i++

        if (tokens[i].tokenType != TokenType.L_CURL) {
            throw IllegalArgumentException("Function body expected " + tokens[i].at())
        }

        val functionEndPosition = findLastCurlBrace(tokens, i)
        if (functionEndPosition == -1) {
            throw IllegalArgumentException("Missing } for function $functionName")
        }

        val function = Function(functionName, argNames)
        function.parse(tokens.subList(i + 1, functionEndPosition), this)
        functions.add(function)
        return functionEndPosition + 1
    }

    private fun parseVar(tokens: List<Token>, start: Int): Int {
        if ((tokens.size < start + 2) || (tokens[start + 1] !is Symbol)) {
            throw IllegalArgumentException("Variable name expected " + tokens[start].at(4))
        }
        val varName = (tokens[start + 1] as Symbol).content
        if (!hasVariable(varName)) {
            globalVars[varName] = Variable(varName)
        } else {
            throw IllegalArgumentException("Variable redefinition: $varName " + tokens[start + 1].at())
        }

        when(tokens[start + 2].tokenType) {
            TokenType.END -> {
                return start + 2
            }
            TokenType.L_SQUARE -> {
                if (tokens[start + 3].tokenType != TokenType.NUMBER) {
                    unexpectedTokenError(tokens[start + 3])
                }
                val size = (tokens[start + 3] as Number).value
                if (tokens[start + 4].tokenType != TokenType.R_SQUARE) {
                    unexpectedTokenError(tokens[start + 4])
                }
                if (tokens[start + 5].tokenType != TokenType.END) {
                    unexpectedTokenError(tokens[start + 5])
                }
                globalVars[varName]?.size = size
                return start + 5
            }
            TokenType.OPERATOR -> {
                val operator = tokens[start + 2] as Operator
                if (operator.type != OperatorType.ASSIGN) {
                    unexpectedTokenError(tokens[start + 2])
                }
                when(tokens[start + 3].tokenType) {
                    TokenType.NUMBER -> {
                        globalVars[varName]?.value = listOf((tokens[start + 3] as Number).value)
                        if (tokens[start + 4].tokenType != TokenType.END) {
                            unexpectedTokenError(tokens[start + 4])
                        }
                        return start + 4
                    }
                    TokenType.L_CURL -> {
                        val j = findLastCurlBrace(tokens, start + 3)
                        if (j == -1) {
                            throw IllegalArgumentException("Missing } " + tokens[start + 3].at())
                        }
                        val literal = parseArrayLiteral(tokens.subList(start + 4, j))
                        globalVars[varName]?.value = literal
                        globalVars[varName]?.size = literal.size
                        if (tokens[j + 1].tokenType != TokenType.END) {
                            unexpectedTokenError(tokens[j + 1])
                        }
                        return j + 1
                    }
                    else -> {
                        unexpectedTokenError(tokens[start + 3])
                        return -1
                    }
                }
            }
            else -> {
                unexpectedTokenError(tokens[start + 2])
                return -1
            }
        }
    }

}