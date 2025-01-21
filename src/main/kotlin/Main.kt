package org.pingwiner.compiler

import org.pingwiner.compiler.builder.BuildConfig
import org.pingwiner.compiler.builder.Builder

fun main() {
    val buildConfig = BuildConfig(
        sourceList = listOf(
            "input.txt",
            "lib.txt"
        ),
        includes = listOf("inc.asm")
    )

    Builder().build(buildConfig)
}