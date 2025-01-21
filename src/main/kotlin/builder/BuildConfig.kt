package org.pingwiner.compiler.builder

data class BuildConfig(
    val sourceList: List<String>,
    val includes: List<String> = listOf(),
    val targetFile: String = "main.asm"
)
