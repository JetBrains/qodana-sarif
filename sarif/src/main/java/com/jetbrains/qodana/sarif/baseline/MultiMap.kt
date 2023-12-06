package com.jetbrains.qodana.sarif.baseline

internal data class MultiMap<K, V> @JvmOverloads constructor(
    private val underlying: MutableMap<K, MutableList<V>> = mutableMapOf()
) : Iterable<Map.Entry<K, List<V>>> {

    fun add(key: K, value: V) {
        underlying.compute(key) { _, old -> old?.apply { add(value) } ?: mutableListOf(value) }
    }

    fun containsKey(key: K) = underlying.containsKey(key)

    fun getOrEmpty(key: K) = underlying[key] ?: emptyList()

    override fun iterator(): Iterator<Map.Entry<K, List<V>>> = underlying.iterator()
}
