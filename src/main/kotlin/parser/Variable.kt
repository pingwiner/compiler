package org.pingwiner.compiler.parser

import org.pingwiner.compiler.*
import org.pingwiner.compiler.Number

class Variable(val name: String) {
    var size: Int = 1
    var value: List<Int>? = null

    fun isArray() = size > 1

    companion object {
        fun parse(tokens: List<Token>, start: Int) : Pair<Variable, Int>? {
            parseUninitializedVar(tokens, start)?.let { return it }
            parseInitializedVar(tokens, start)?.let { return it }
            parseUninitializedArray(tokens, start)?.let { return it }
            parseInitializedArray(tokens, start)?.let { return it }
            return null
        }

        //var a;
        private fun parseUninitializedVar(tokens: List<Token>, start: Int) : Pair<Variable, Int>? {
            Match(tokens, start)
                .expect<Keyword.Var>()
                .expect<Symbol>()
                .expect<SpecialSymbol.End>().let {
                    if (!it.valid) return null
                    val result = Variable(it.get<Symbol>(1).content)
                    return Pair(result, it.next)
                }
        }

        //var a = 1;
        private fun parseInitializedVar(tokens: List<Token>, start: Int) : Pair<Variable, Int>? {
            Match(tokens, start)
                .expect<Keyword.Var>()
                .expect<Symbol>()
                .expect<Operator.Assign>()
                .expect<Number>()
                .expect<SpecialSymbol.End>().let {
                    if (!it.valid) return null
                    val result = Variable(it.get<Symbol>(1).content)
                    result.value = listOf(it.get<Number>(3).value)
                    result.size = 1
                    return Pair(result, it.next)
                }
        }

        //var arr[10];
        private fun parseUninitializedArray(tokens: List<Token>, start: Int) : Pair<Variable, Int>? {
            Match(tokens, start)
                .expect<Keyword.Var>()
                .expect<Symbol>()
                .expect<SpecialSymbol.LSquare>()
                .expect<Number>()
                .expect<SpecialSymbol.RSquare>()
                .expect<SpecialSymbol.End>().let {
                    if (!it.valid) return null
                    val result = Variable(it.get<Symbol>(1).content)
                    result.size = it.get<Number>(3).value
                    return Pair(result, it.next)
                }
        }

        //var arr = {1, 2, 3};
        private fun parseInitializedArray(tokens: List<Token>, start: Int) : Pair<Variable, Int>? {
            Match(tokens, start)
                .expect<Keyword.Var>()
                .expect<Symbol>()
                .expect<Operator.Assign>()
                .expect<SpecialSymbol.LCurl>()
                .expect<Number>().let {
                    if (!it.valid) return null
                    val result = Variable(it.get<Symbol>(1).content)
                    val j = findComplementBraceToken<SpecialSymbol.LCurl, SpecialSymbol.RCurl>(tokens, start + 3)
                    if (j == -1) return null
                    val literal = parseArrayLiteral(tokens.subList(start + 4, j))
                    result.value = literal
                    result.size = literal.size
                    return Pair(result, j + 1)
                }
        }
    }
}