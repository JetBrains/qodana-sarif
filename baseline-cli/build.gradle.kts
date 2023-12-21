plugins {
    id("qodana-sarif.common-conventions")
    alias(libs.plugins.shadow)
    id ("me.champeau.jmh") version "0.7.2"
}

dependencies {
    implementation(projects.sarif)
    implementation(libs.gson)

    jmh("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}

application {
    mainClass.set("MainKt")
}

tasks {
    shadowJar {
        archiveBaseName.set("baseline-cli")
        archiveVersion.set("0.1.0")
        archiveClassifier.set("")
        destinationDirectory.set(file("lib"))
    }
}
