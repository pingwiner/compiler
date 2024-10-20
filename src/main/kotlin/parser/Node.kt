package org.pingwiner.compiler.parser

import org.pingwiner.compiler.Token

enum class NodeType {
    Regular,
    Function,
    ArrayAccess,
    Block
}

class Node() {
    var value: Token? = null
    var subNodes: List<Node>? = null
    var type = NodeType.Regular

    constructor(token: Token) : this() {
        value = token
    }

    constructor(nodes: List<Node>) : this() {
        subNodes = nodes
    }

    override fun toString(): String {
        return "value: " + value?.toString() + ", " + if (subNodes != null) printNodes(subNodes!!) else ""
    }
}
