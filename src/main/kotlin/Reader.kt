package org.pingwiner.compiler

import java.io.BufferedReader
import java.io.File

class Reader(fileName: String) {
    private val bufferedReader: BufferedReader = File(fileName).bufferedReader()
    val content = bufferedReader.use { it.readText() }
}