import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.pingwiner.compiler.Number
import org.pingwiner.compiler.Lexer
import org.pingwiner.compiler.Keyword
import org.pingwiner.compiler.Symbol
import org.pingwiner.compiler.Operator
import org.pingwiner.compiler.SpecialSymbol
import kotlin.test.assertIs

class LexerTest {

    @Test
    fun scan() {
        val lexer = Lexer()
        lexer.scan("var abc = 123;")
        assertEquals(5, lexer.tokens.size)

        assertIs<Keyword.Var>(lexer.tokens[0])
        assertIs<Symbol>(lexer.tokens[1])
        assertIs<Operator.Assign>(lexer.tokens[2])
        assertIs<Number>(lexer.tokens[3])
        assertIs<SpecialSymbol.End>(lexer.tokens[4])

        assertEquals((lexer.tokens[1] as Symbol).content, "abc")
        assertEquals((lexer.tokens[3] as Number).value, 123)

        lexer.reset()
        lexer.scan("var arr = {1, 2, 3};")
        assertEquals(11, lexer.tokens.size)

        assertIs<SpecialSymbol.LCurl>(lexer.tokens[3])
        assertIs<Number>(lexer.tokens[4])
        assertIs<SpecialSymbol.Comma>(lexer.tokens[5])
        assertIs<Number>(lexer.tokens[6])
        assertIs<SpecialSymbol.Comma>(lexer.tokens[7])
        assertIs<Number>(lexer.tokens[8])
        assertIs<SpecialSymbol.RCurl>(lexer.tokens[9])
        assertIs<SpecialSymbol.End>(lexer.tokens[10])

        lexer.reset()
        lexer.scan("fun f1(x, y) { return 1; }")
        assertEquals(13, lexer.tokens.size)

        assertIs<Keyword.Fun>(lexer.tokens[0])
        assertIs<Symbol>(lexer.tokens[1])
        assertIs<SpecialSymbol.LBrace>(lexer.tokens[2])
        assertIs<Symbol>(lexer.tokens[3])
        assertIs<SpecialSymbol.Comma>(lexer.tokens[4])
        assertIs<Symbol>(lexer.tokens[5])
        assertIs<SpecialSymbol.RBrace>(lexer.tokens[6])
        assertIs<SpecialSymbol.LCurl>(lexer.tokens[7])
        assertIs<Keyword.Return>(lexer.tokens[8])
        assertIs<Operator.Assign>(lexer.tokens[9])
        assertIs<Number>(lexer.tokens[10])
        assertIs<SpecialSymbol.End>(lexer.tokens[11])
        assertIs<SpecialSymbol.RCurl>(lexer.tokens[12])

        lexer.reset()
        lexer.scan("a[i] = 1 + 2/3 - 4 *6 & (c% d) | 0 ^ 3;")
        assertEquals(25, lexer.tokens.size)

        assertIs<Symbol>(lexer.tokens[0])
        assertIs<SpecialSymbol.LSquare>(lexer.tokens[1])
        assertIs<Symbol>(lexer.tokens[2])
        assertIs<SpecialSymbol.RSquare>(lexer.tokens[3])
        assertIs<Operator.Plus>(lexer.tokens[6])
        assertIs<Operator.Divide>(lexer.tokens[8])
        assertIs<Operator.Minus>(lexer.tokens[10])
        assertIs<Operator.Multiply>(lexer.tokens[12])
        assertIs<Operator.And>(lexer.tokens[14])
        assertIs<Operator.Mod>(lexer.tokens[17])
        assertIs<Operator.Or>(lexer.tokens[20])
        assertIs<Operator.Xor>(lexer.tokens[22])

        lexer.reset()
        lexer.scan("a = ((x > 1) | (y < 2)) ? 1 : 2;")
        assertEquals(20, lexer.tokens.size)

        assertIs<SpecialSymbol.LBrace>(lexer.tokens[2])
        assertIs<SpecialSymbol.LBrace>(lexer.tokens[3])
        assertIs<Operator.Gt>(lexer.tokens[5])
        assertIs<SpecialSymbol.RBrace>(lexer.tokens[7])
        assertIs<Operator.Lt>(lexer.tokens[11])
        assertIs<Operator.If>(lexer.tokens[15])
        assertIs<Operator.Else>(lexer.tokens[17])

        lexer.reset()
        lexer.scan("{x=x+1;} while (x <= 10); {x=x-1;} until (x>=0);")
        assertEquals(30, lexer.tokens.size)

        assertIs<Operator.While>(lexer.tokens[8])
        assertIs<Operator.Until>(lexer.tokens[23])
        assertIs<Operator.LtEq>(lexer.tokens[11])
        assertIs<Operator.GtEq>(lexer.tokens[26])

        lexer.reset()
        lexer.scan("(a != 2) ? a << 1 : a >> 2;")
        assertEquals(14, lexer.tokens.size)

        assertIs<Operator.Neq>(lexer.tokens[2])
        assertIs<Operator.Shl>(lexer.tokens[7])
        assertIs<Operator.Shr>(lexer.tokens[11])
    }
}