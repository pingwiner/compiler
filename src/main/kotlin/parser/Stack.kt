package org.pingwiner.compiler.parser

class Stack<T> {
    private val deq = ArrayDeque<T>()

    fun push(v: T) {
        deq.addFirst(v)
    }

    fun pop(): T? {
        return if (deq.size > 0) {
            deq.removeFirst()
        } else null
    }

    fun isEmpty() = deq.isEmpty()
}