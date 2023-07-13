# qodana-sarif

This Java library contains classes describing the SARIF (Static Analysis Results Interchange Format) and utilities to work with them.

## Features

- **Lazy Reading**: The library supports a lazy reading format of SARIF, which allows the report to be read and processed gradually, potentially reducing memory consumption.

- **Full Report Reading**: The library also supports reading the full SARIF report simultaneously. Be aware this is CPU and memory intensive.

- **Flexible Result Analysis**: The `com.jetbrains.qodana.sarif.model.Result` object provides several properties which can be used to filter and analyze the issues in the report.

## Usage

### Adding to your project

Add the following to the `build.gradle.kts`:

```kotlin
val qodanaSarifVersion = "0.2.8"

repositories {
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

dependencies {
    implementation("com.jetbrains.qodana:qodana-sarif:$qodanaSarifVersion")
}
```


### Lazy Reading

```kotlin
fun getProblems(): Sequence<com.jetbrains.qodana.sarif.model.Result> = sequence {
    java.io.FileReader(filename).use { fileReader ->
        for (indexedResult in com.jetbrains.qodana.sarif.SarifUtil.lazyReadIndexedResults(fileReader)) {
            yield(indexedResult.result)
        }
    }
}
```

### Full Report Reading

```kotlin
fun readReport(): com.jetbrains.qodana.sarif.model.SarifReport {
    return java.io.FileReader(filename).use { fileReader ->
        com.jetbrains.qodana.sarif.SarifUtil.readReport(fileReader)
    }
}
```

### Result Properties

You can use one of the following `com.jetbrains.qodana.sarif.model.Result` properties in your utilities:

- `com.jetbrains.qodana.sarif.model.Result#getBaselineState` Nullable baseline state - one of (new, updated, absent, unchanged)
- `com.jetbrains.qodana.sarif.model.Result#getRuleId` Rule id that raised the problem
- `com.jetbrains.qodana.sarif.model.Result#getLevel` Severity level of the issue
