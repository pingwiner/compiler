package parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.pingwiner.compiler.Lexer
import org.pingwiner.compiler.Reader
import org.pingwiner.compiler.parser.Parser

class ParserTest {

    @Test
    fun parse() {
        val reader = Reader("test/test1.src")
        val lexer = Lexer()
        lexer.scan(reader.content)
        val parser = Parser()
        val program = parser.parse(lexer.tokens)
        assertEquals(4, program.globalVars.size)
        assertEquals(1, program.functions.size)
        assertEquals("main", program.functions[0].name)
    }
}