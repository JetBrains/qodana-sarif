dependencies {
    implementation("com.google.code.gson:gson:2.8.6")
    testImplementation("junit:junit:4.12")
}

publishing {
    publications {
        create<MavenPublication>("sarif") {
            val minorVersion: String by project

            groupId = project.group.toString()
            artifactId = project.name
            version = "0.1.$minorVersion"

            from(components["java"])

            pom {
                developers {
                    developer {
                        id.set("nikitaKochetkov")
                        name.set("Nikita Kochetkov")
                        email.set("Nikita.Kochetkov@jetbrains.com")
                    }
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
            val spaceLogin: String by rootProject.extra
            val spacePassword: String by rootProject.extra
            val spaceArtifactoryUrl: String by project

            url = uri(spaceArtifactoryUrl)
            credentials {
                username = spaceLogin
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