plugins {
    id("qodana-sarif.common-conventions")
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.sarif)
    implementation(libs.gson)
    implementation("com.github.ajalt.clikt:clikt:4.3.0")
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
