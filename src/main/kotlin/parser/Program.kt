package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*

class Program {
    val functions = mutableListOf<Function>()
    val globalVars = mutableListOf<String>()

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
    }

    private fun parseFunction(tokens: List<Token>, start: Int): Int {
        if ((tokens.size < start + 2) || (tokens[start + 1] !is Symbol)) {
            throw IllegalArgumentException("Function name expected at line " + tokens[start].line + ", position " + tokens[start].position + 4)
        }
        val functionName = (tokens[start + 1] as Symbol).content

        if (tokens[start + 2].tokenType != TokenType.L_BRACE) {
            throw IllegalArgumentException("Function parameters expected at line " + tokens[start + 2].line + ", position " + tokens[start + 2].position)
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
            throw IllegalArgumentException("Function body expected at line " + tokens[i].line + ", position " + tokens[i].position)
        }

        val functionEndPosition = findLastBrace(tokens, i)
        if (functionEndPosition == -1) {
            throw IllegalArgumentException("Missing } for function $functionName")
        }

        val function = Function(functionName, argNames)
        function.parse(tokens.subList(i + 1, functionEndPosition))
        functions.add(function)
        return functionEndPosition + 1
    }

    private fun parseVar(tokens: List<Token>, start: Int): Int {
        if ((tokens.size < start + 2) || (tokens[start + 1] !is Symbol)) {
            throw IllegalArgumentException("Variable name expected at line " + tokens[start].line + ", position " + tokens[start].position + 4)
        }
        val varName = (tokens[start + 1] as Symbol).content
        globalVars.add(varName)
        return start + 2
    }

    private fun unexpectedTokenError(token: Token) {
        throw IllegalArgumentException("Unexpected token at line " + token.line + ", position " + token.position)
    }

}