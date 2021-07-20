import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.5.21"

}

repositories {
    jcenter()
}

dependencies {
    testImplementation("junit:junit:4.12")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation(kotlin("stdlib-jdk8"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.jetbrains.qodana"
            artifactId = "qodana-sarif"
            val minorVersion: String by project
            version = "0.1.$minorVersion"
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            val deployUsername: String by project
            val deployPassword: String by project

            credentials {
                this.username = deployUsername
                this.password = deployPassword
            }
        }
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}