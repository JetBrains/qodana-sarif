plugins {
    id("qodana-sarif.common-conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(projects.sarif)
    implementation(libs.gson)
    implementation(libs.clikt)
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
