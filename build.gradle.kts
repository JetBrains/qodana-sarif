plugins {
    `java-library`
    `maven-publish`
    application
    kotlin("jvm") version "1.3.72"
}

val spaceUser: String by project
val spacePasswordToken: String by project

val kotlinVersion by extra("1.3.72")
val spaceLogin by extra(projectSettingsValue("spaceLogin", spaceUser))
val spacePassword by extra(projectSettingsValue("spacePassword", spacePasswordToken))


allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "application")

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
                username = spaceLogin
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

        version = kotlinVersion
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}


fun projectSettingsValue(extraParameter: String, propertiesParameter: String): String {
    return project.findProperty(extraParameter)?.toString()?.ifBlank { null } ?: propertiesParameter
}