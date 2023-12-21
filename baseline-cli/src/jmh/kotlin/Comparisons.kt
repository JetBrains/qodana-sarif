package com.jetbrains.qodana.sarif.jmh

import com.jetbrains.qodana.sarif.baseline.BaselineCalculation
import com.jetbrains.qodana.sarif.baseline.OldCalculation
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@Warmup(time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(time = 5, timeUnit = TimeUnit.SECONDS, iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class Comparisons {

    private fun runNew(baselinePercentage: Int, reportPercentage: Int) =
        BaselineCalculation.compare(
            TestData.read(reportPercentage, "report"),
            TestData.read(baselinePercentage, "baseline"),
            BaselineCalculation.Options(true)
        )

    private fun runOld(baselinePercentage: Int, reportPercentage: Int) =
        OldCalculation.compare(
            TestData.read(reportPercentage, "report"),
            TestData.read(baselinePercentage, "baseline"),
            OldCalculation.Options(true)
        )

    @Param(value = ["1", "10", "30", "50", "100"])
    var baselinePercentage: Int = -1

    @Param(value = ["1", "10", "30", "50", "100"])
    var reportPercentage: Int = -1

    @Param(value = ["old", "new"])
    var algo: String = ""


    @Benchmark
    fun bench(): Any? {
        check(baselinePercentage != -1)
        check(reportPercentage != -1)
        check(algo.isNotBlank())

        return when (algo) {
            "old" -> runOld(baselinePercentage, reportPercentage)
            "new" -> runNew(baselinePercentage, reportPercentage)
            else -> error("unexpected algo $algo")
        }

    }

}
