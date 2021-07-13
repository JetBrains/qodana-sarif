plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.5.21"
}

val spaceUser: String by project
val spacePasswordToken: String by project

val kotlinVersion by extra("1.5.21")
val spaceLogin by extra(projectSettingsValue("spaceLogin", spaceUser))
val spacePassword by extra(projectSettingsValue("spacePassword", spacePasswordToken))


allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")

    group = "com.jetbrains.qodana"

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


fun projectSettingsValue(extraParameter: String, propertiesParameter: String): String {
    return project.findProperty(extraParameter)?.toString()?.ifBlank { null } ?: propertiesParameter
}