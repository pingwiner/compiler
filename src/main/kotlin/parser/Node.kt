package org.pingwiner.compiler.parser

import org.pingwiner.compiler.Token

class Node() {
    var value: Token? = null
    var subNodes: List<Node>? = null

    constructor(token: Token) : this() {
        value = token
    }

    constructor(nodes: List<Node>) : this() {
        subNodes = nodes
    }
}
