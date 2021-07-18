val archiveJarName: String by project

val jarArchiveName by extra(projectSettingsValue("archiveJarName", archiveJarName))
val kotlinVersion = project.rootProject.ext.properties["kotlinVersion"] as String

dependencies {
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("commons-cli", "commons-cli", "1.4")
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")
    implementation("org.apache.logging.log4j:log4j-api:2.14.0")
    implementation("org.jetbrains.teamcity.qodana:teamcity-common:0.10.57")
    implementation(project(":sarif"))

    implementation(kotlin("stdlib", kotlinVersion))

    testImplementation("junit:junit:4.12")
}

tasks.register<Jar>("sarifConverter-fatJar") {
    dependsOn(tasks.build, tasks.test)

    archiveVersion.set("")
    archiveFileName.set("$jarArchiveName.jar")
    archiveClassifier.set("sarifConverter")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes("Main-Class" to "com.jetbrains.qodana.sarif.app.Main", "Multi-Release" to "true")
    }

    val sourcesMain = sourceSets.main.get()
    from(sourcesMain.output)
}


publishing {
    publications {
        create<MavenPublication>("sarifConverter") {
            val minorVersion: String by project

            groupId = project.group.toString()
            artifactId = project.name
            version = "0.1.$minorVersion"

            artifact(tasks.getByName("sarifConverter-fatJar"))

            pom {
                developers {
                    developer {
                        id.set("nikitaKochetkov")
                        name.set("Nikita Kochetkov")
                        email.set("Nikita.Kochetkov@jetbrains.com")
                    }
                }
            }
        }
    }
    repositories {
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

fun projectSettingsValue(extraParameter: String, propertiesParameter: String): String {
    return project.findProperty(extraParameter)?.toString()?.ifBlank { null } ?: propertiesParameter
}