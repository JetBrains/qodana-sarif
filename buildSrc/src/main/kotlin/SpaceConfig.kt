import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.provideDelegate

fun RepositoryHandler.space(
    project: Project,
) = maven {
    val spaceArtifactoryUrl: String by project
    val spaceUsername: String by project
    val spacePassword: String by project

    name = "Space"
    url = project.uri(spaceArtifactoryUrl)

    credentials {
        username = spaceUsername
        password = spacePassword
    }
}
