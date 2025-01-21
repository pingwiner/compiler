package org.pingwiner.compiler.builder

import org.pingwiner.compiler.Lexer
import org.pingwiner.compiler.Reader
import org.pingwiner.compiler.codegen.IrGenerator
import org.pingwiner.compiler.parser.Parser
import java.io.File

class Builder {
    fun build(config: BuildConfig) {
        val lexer = Lexer()
        for (src in config.sourceList) {
            val reader = Reader(src)
            lexer.scan(reader.content)
        }
        val parser = Parser()
        val program = parser.parse(lexer.tokens)
        val irGenerator = IrGenerator(program)
        val sb = StringBuilder()
        sb.appendLine(irGenerator.generate())
        for (inc in config.includes) {
            sb.appendLine(".INCLUDE \"$inc\"")
        }
        File(config.targetFile).writeText(sb.toString())
    }
}