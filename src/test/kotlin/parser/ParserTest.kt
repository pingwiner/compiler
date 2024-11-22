package parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.pingwiner.compiler.Lexer
import org.pingwiner.compiler.Reader
import org.pingwiner.compiler.parser.ASTNode
import org.pingwiner.compiler.parser.Parser
import kotlin.test.assertIs

class ParserTest {

    @Test
    fun parse() {
        val reader = Reader("test/test1.src")
        val lexer = Lexer()
        lexer.scan(reader.content)
        val parser = Parser()
        val program = parser.parse(lexer.tokens)
        assertEquals(4, program.globalVars.size)
        assertEquals(2, program.functions.size)

        assertEquals(1, program.globalVars["a"]?.size)
        assertEquals(1, program.globalVars["b"]?.size)
        assertEquals(3, program.globalVars["arr1"]?.size)
        assertEquals(10, program.globalVars["arr2"]?.size)

        assertEquals("f1", program.functions[0].name)
        assertEquals("main", program.functions[1].name)
        assertEquals(2, program.functions[0].params.size)
        assertEquals(0, program.functions[1].params.size)
        assertEquals(0, program.functions[0].vars.size)
        assertEquals(1, program.functions[1].vars.size)

        assertIs<ASTNode.BinaryOperation.If>(program.functions[0].root)
        val root = program.functions[0].root as ASTNode.BinaryOperation.If
        assertIs<ASTNode.BinaryOperation.Else>(root.right)
        assertIs<ASTNode.BinaryOperation.Gt>(root.left)
        val mainRoot = program.functions[1].root as ASTNode.Block
        assertIs<ASTNode.BinaryOperation.Assign>(mainRoot.subNodes[0])
        assertIs<ASTNode.BinaryOperation.Assign>(mainRoot.subNodes[1])
        assertIs<ASTNode.Return>(mainRoot.subNodes[2])
    }
}