package com.jetbrains.qodana.sarif.jmh

import com.google.gson.JsonElement
import com.jetbrains.qodana.sarif.SarifUtil
import com.jetbrains.qodana.sarif.model.SarifReport
import kotlinx.coroutines.*
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.reader
import kotlin.random.Random
import kotlin.time.TimeSource

object TestData {
    private val resources = Path("/home/johannes/sources/qodana-sarif/baseline-cli/src/jmh/resources")
    private val gson = SarifUtil.createGson()

    private val start = TimeSource.Monotonic.markNow()

    private fun log(msg: String) {
        println("[${start.elapsedNow()}] $msg")
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val originalJs = resources.resolve("ij_run_orig.json")
            .reader()
            .use { r -> gson.fromJson(r, JsonElement::class.java) }
        log("Read original report")

        sequenceOf("report", "baseline")
            .flatMap { p -> intArrayOf(1, 10, 30, 50, 100).map { p to it } }
            .forEach { (prefix, percentage) -> launch(Dispatchers.Default) { copy(originalJs, percentage, prefix) } }
    }

    private suspend fun copy(src: JsonElement, keepPercentage: Int, prefix: String) {
        val name = "$prefix-$keepPercentage.json"

        log("Generating data for $name")
        val report = runInterruptible {
            gson.fromJson(src, SarifReport::class.java)
                .apply { runs.forEach { r -> r.results.removeIf { Random.nextInt(99) >= keepPercentage } } }
        }

        log("Writing data for $name")
        runInterruptible(Dispatchers.IO) {
            resources.resolve(name)
                .bufferedWriter()
                .use { gson.toJson(report, it) }
        }
        log("Generated $name")
    }

    fun read(keepPercentage: Int, prefix: String): SarifReport =
        resources.resolve("$prefix-$keepPercentage.json")
            .bufferedReader()
            .use { gson.fromJson(it, SarifReport::class.java) }

}
