val archiveJarName: String by project

val jarArchiveName by extra(projectSettingsValue("archiveJarName", archiveJarName))

dependencies {
    implementation("com.google.code.gson:gson:2.8.6")
    implementation(project(":sarif"))
    testImplementation("junit:junit:4.12")
}

tasks.register<Jar>("sarifConverter-fatJar") {
    dependsOn(tasks.build, tasks.test)

    archiveVersion.set("")
    archiveFileName.set("$jarArchiveName.jar")
    archiveClassifier.set("sarifConverter")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes("Main-Class" to "com.jetbrains.qodana.sarif.converter.Main", "Multi-Release" to "true")
    }

    val sourcesMain = sourceSets.main.get()
    from(sourcesMain.output)
}


fun projectSettingsValue(extraParameter: String, propertiesParameter: String): String {
    return project.findProperty(extraParameter)?.toString()?.ifBlank { null } ?: propertiesParameter
}