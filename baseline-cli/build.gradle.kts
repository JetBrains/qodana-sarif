plugins {
    id("qodana-sarif.common-conventions")
    alias(libs.plugins.shadow)
}

val artifactName = "baseline-cli"
val tcBuildVersion : String? by project

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
        archiveBaseName.set(artifactName)
        archiveVersion.set(tcBuildVersion)
        archiveClassifier.set("")
        destinationDirectory.set(file("lib"))
    }
}

publishing {
    publications {
        create<MavenPublication>(artifactName) {
            groupId = project.group.toString()
            artifactId = artifactName
            version = tcBuildVersion

            artifact(tasks.shadowJar)
        }
    }

    repositories {
        maven {
            name = "static-analysis"
            val spaceUsername: String? by project
            val spacePassword: String? by project
            url = uri("https://packages.jetbrains.team/maven/p/sa/static-analysis")
            credentials {
                username = spaceUsername
                password = spacePassword
            }
        }

        maven {
            name = "intellij-dependencies"
            val spaceUsername: String? by project
            val spacePassword: String? by project
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            credentials {
                username = spaceUsername
                password = spacePassword
            }
        }
    }
}