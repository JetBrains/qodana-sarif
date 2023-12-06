package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.Run
import com.jetbrains.qodana.sarif.model.VersionedMap
import com.jetbrains.qodana.sarif.model.Result as SarifResult

private typealias Fingerprint = String
private typealias FingerprintIndex = MultiMap<Fingerprint, SarifResult>
private typealias KeyIndex = MultiMap<ResultKey, SarifResult>

internal fun createIndices(
    run: Run,
    selectPrints: (VersionedMap<String>) -> List<Fingerprint?>
): Pair<FingerprintIndex, KeyIndex> {
    val printIndex = FingerprintIndex()
    val keyIndex = KeyIndex()
    run.results.noNulls()
        .filterNot { it.baselineState == Result.BaselineState.ABSENT }
        .forEach { result ->
            result.partialFingerprints?.let(selectPrints)
                ?.forEach {  print -> if (print != null) printIndex.add(print, result) }
                ?: keyIndex.add(ResultKey(result), result)
        }

    return printIndex to keyIndex
}

private fun <T : Any> Iterable<T?>?.noNulls(): Sequence<T> =
    this?.asSequence().orEmpty().filterNotNull()
