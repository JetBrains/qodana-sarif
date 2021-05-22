plugins {
    `java-library`
    `maven-publish`

}

repositories {
    jcenter()
}

dependencies {
    testImplementation("junit:junit:4.12")
    implementation("com.google.code.gson:gson:2.8.6")
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