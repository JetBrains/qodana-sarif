import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URL

plugins {
    `java-library`
    `maven-publish`
    jacoco
    application
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.dokka") version "1.8.20"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.8.20")
    }
}

val spaceUsername: String by project
val spacePassword: String by project

val kotlinVersion by extra("1.8.0")
//val spaceLogin by extra(projectSettingsValue("spaceLogin", spaceUser))
//val spacePassword by extra(projectSettingsValue("spacePassword", spacePasswordToken))


allprojects {
    apply(plugin = "jacoco")
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "application")
    apply(plugin = "org.jetbrains.dokka")

    group = "com.jetbrains.qodana"

    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
        maven(url = "https://repo.labs.intellij.net/thirdparty")

        maven {
            name = "Space"
            val spaceArtifactoryUrl: String by project
            url = uri(spaceArtifactoryUrl)

            credentials {
                username = spaceUsername
                password = spacePassword
            }
        }
    }

    kotlin {
        this.sourceSets {
            main {
                kotlin.srcDirs("src/main/java")
                kotlin.srcDirs("src/main/kotlin")
                resources.srcDirs("src/main/resources")
            }

            test {
                kotlin.srcDirs("src/test/java")
                kotlin.srcDirs("src/test/kotlin")
                resources.srcDirs("src/test/resources")
            }
        }

        version = "0.2.8"
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            moduleName.set(project.name)
            sourceRoots.from(file("src"))
            includes.from("README.md")
            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URL("https://github.com/JetBrains/qodana-sarif/tree/main/sarif/src"))
                remoteLineSuffix.set("#L")
            }
            sourceSets {
                skipEmptyPackages.set(true)
            }
            pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
                footerMessage = "(c) 2023 JetBrains s.r.o."
            }
        }
    }
}


fun projectSettingsValue(extraParameter: String, propertiesParameter: String): String {
    return project.findProperty(extraParameter)?.toString()?.ifBlank { null } ?: propertiesParameter
}