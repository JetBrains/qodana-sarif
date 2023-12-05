import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    id("qodana-sarif.common-conventions")
    alias(libs.plugins.dokka)
}

buildscript {
    dependencies {
        classpath(libs.dokkaBase)
    }
}

dependencies {
    implementation(libs.gson)
}

tasks {
    withType<DokkaTaskPartial> {
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

    withType<AbstractPublishToMaven> {
        dependsOn(test)
    }

    test {
        finalizedBy(jacocoTestReport)
    }
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
        space(project)
    }
}
