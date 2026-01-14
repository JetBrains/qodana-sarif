plugins {
    kotlin("jvm")
    jacoco
    application
    `maven-publish`
}

group = "com.jetbrains.qodana"

kotlin {
    jvmToolchain(8)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://repo.labs.intellij.net/thirdparty")

    space(project)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    val junit = "5.8.1"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
}
