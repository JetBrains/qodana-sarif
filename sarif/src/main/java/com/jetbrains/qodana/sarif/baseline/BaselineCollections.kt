package com.jetbrains.qodana.sarif.baseline

import java.util.*

internal class MultiMap<K, V> private constructor(
    private val underlying: MutableMap<K, MutableList<V>>
) : Iterable<Map.Entry<K, List<V>>> {
    constructor() : this(mutableMapOf())

    fun add(key: K, value: V) {
        underlying.compute(key) { _, old -> old?.apply { add(value) } ?: mutableListOf(value) }
    }

    fun getOrEmpty(key: K): MutableList<V> = underlying[key] ?: mutableListOf()

    override fun iterator(): Iterator<Map.Entry<K, List<V>>> = underlying.iterator()
}

internal class IdentitySet<T> private constructor(
    private val map: IdentityHashMap<T, Int>
) : MutableSet<T> by map.keys {
    constructor(expectedSize: Int) : this(IdentityHashMap(expectedSize))

    private var insertCounter = 0

    override fun add(element: T): Boolean = map.putIfAbsent(element, insertCounter++) == null
    override fun addAll(elements: Collection<T>): Boolean = elements.fold(false) { r, e -> add(e) || r }

    override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
        private val underlying = map.entries.sortedBy(MutableMap.MutableEntry<T, Int>::value).iterator()

        override fun hasNext(): Boolean = underlying.hasNext()
        override fun next(): T = underlying.next().key
        override fun remove() = throw UnsupportedOperationException("Cannot remove elements from this iterator")
    }
}

internal class Counter<T> {
    private val underlying: MutableMap<T, Int> = mutableMapOf()

    operator fun get(key: T) = underlying.getOrDefault(key, 0)
    fun increment(key: T) = underlying.compute(key) { _, o -> (o ?: 0).inc() }!!
    fun decrement(key: T) = underlying.compute(key) { _, o -> (o ?: 0).dec() }!!
}
