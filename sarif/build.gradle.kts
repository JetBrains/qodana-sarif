dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    testImplementation("junit:junit:4.13.1")
}

publishing {
    publications {
        create<MavenPublication>("sarif") {
            val majorVersion: String by project
            val minorVersion: String by project
            val patch: String by project

            groupId = project.group.toString()
            artifactId = "qodana-sarif"
            version = "$majorVersion.$minorVersion.$patch"

            from(components["java"])

            pom {
                developers {
                    developer {
                        id.set("alexeyAfanasiev")
                        name.set("Alexey Afanasiev")
                        email.set("Alexey.Afanasiev@jetbrains.com")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "intellij-dependencies"
            val deployUsername: String by project
            val deployPassword: String by project

            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            credentials {
                username = deployUsername
                password = deployPassword
            }
        }
        maven {
            name = "Space"
            val spaceUsername: String by project
            val spacePassword: String by project
            val spaceArtifactoryUrl: String by project

            url = uri(spaceArtifactoryUrl)
            credentials {

                username = spaceUsername
                password = spacePassword
            }
        }

    }
}


tasks.named("publishAllPublicationsToIntellij-dependenciesRepository") {
    dependsOn(tasks.test)
}

tasks.named("publishAllPublicationsToSpaceRepository") {
    dependsOn(tasks.test)
}

tasks.named("publishSarifPublicationToIntellij-dependenciesRepository") {
    dependsOn(tasks.test)
}

tasks.named("publishSarifPublicationToSpaceRepository") {
    dependsOn(tasks.test)
}