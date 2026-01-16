import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    id("qodana-sarif.common-conventions")
    alias(libs.plugins.dokka)
}

val artifactName = "qodana-sarif"
val tcBuildVersion: String? by project

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

    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
    withType<AbstractPublishToMaven> {
        dependsOn(test)
    }

    test {
        finalizedBy(jacocoTestReport)
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>(artifactName) {
            groupId = project.group.toString()
            artifactId = artifactName
            version = tcBuildVersion

            from(components["java"])

            pom {
                url.set("https://github.com/JetBrains/qodana-sarif")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://github.com/JetBrains/qodana-sarif/blob/main/LICENSE")
                    }
                }

                developers {  // https://github.com/JetBrains/qodana-sarif/graphs/contributors
                    developer {
                        id.set("avafanasiev")
                        name.set("Alexey Afanasiev")
                        email.set("Alexey.Afanasiev@jetbrains.com")
                    }
                    developer {
                        id.set("hybloid")
                        name.set("Dmitry Golovinov")
                        email.set("Dmitry.Golovinov@jetbrains.com")
                    }
                    developer {
                        id.set("jckoenen")
                        name.set("Johannes Koenen")
                        email.set("Johannes.Koenen@jetbrains.com")
                    }
                    developer {
                        id.set("tiulpin")
                        name.set("Viktor Tiulpin")
                        email.set("Viktor.Tiulpin@jetbrains.com")
                    }
                }
            }
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
