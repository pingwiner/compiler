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
            when(val token = tokens[i]) {
                is Keyword -> {
                    when(token) {
                        is Keyword.Fun -> {
                            i = parseFunction(tokens, i)
                        }
                        is Keyword.Var -> {
                            i = parseVar(tokens, i)
                        }
                        else -> {
                            unexpectedTokenError(token)
                        }
                    }
                }
                else -> {
                    if (token is SpecialSymbol.End) {
                        i++
                    } else {
                        unexpectedTokenError(token)
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

        if (tokens[start + 2] !is SpecialSymbol.LBrace) {
            throw IllegalArgumentException("Function parameters expected " + tokens[start + 2].at())
        }

        val closingBracePosition = findComplementBraceToken<SpecialSymbol.LBrace, SpecialSymbol.RBrace>(tokens, start + 2)
        if (closingBracePosition == NOT_FOUND) {
            throw IllegalArgumentException(") not found " + tokens[start + 2].at())
        }

        val argNames = parseFunctionArguments(tokens.subList(start + 3, closingBracePosition))
        val bodyStartingPosition = closingBracePosition + 1

        if (tokens[bodyStartingPosition] !is SpecialSymbol.LCurl) {
            throw IllegalArgumentException("Function body expected " + tokens[bodyStartingPosition].at())
        }

        val functionEndPosition = findComplementBraceToken<SpecialSymbol.LCurl, SpecialSymbol.RCurl>(tokens, bodyStartingPosition)
        if (functionEndPosition == NOT_FOUND) {
            throw IllegalArgumentException("Missing } for function $functionName")
        }

        val function = Function(functionName, argNames)
        function.parse(tokens.subList(bodyStartingPosition + 1, functionEndPosition), this)
        functions.add(function)
        return functionEndPosition + 1
    }

    private fun parseVar(tokens: List<Token>, start: Int): Int {
        val parseResult = Variable.parse(tokens, start)
            ?: throw IllegalArgumentException("Syntax error " + tokens[start].at())

        val varName = parseResult.first.name
        if (!hasVariable(varName)) {
            globalVars[varName] = parseResult.first
        } else {
            throw IllegalArgumentException("Variable redefinition: $varName " + tokens[start + 1].at())
        }

        return parseResult.second
    }

}