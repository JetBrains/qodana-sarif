package com.jetbrains.qodana.sarif

import com.google.common.hash.HashCode
import com.google.common.hash.Hasher


@Suppress("UnstableApiUsage")
class NullableHasher(val hasher: Hasher) {
    private var updated = false

    fun putUnencodedChars(chars: CharSequence): NullableHasher {
        updated = true
        hasher.putUnencodedChars(chars)
        return this
    }

    fun hash(): HashCode? {
        return when (updated) {
            true -> hasher.hash()
            else -> null
        }
    }
}
