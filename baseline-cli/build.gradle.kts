plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

dependencies {
    implementation(project(mapOf("path" to ":sarif")))
    implementation("com.google.code.gson:gson:2.8.9")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
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