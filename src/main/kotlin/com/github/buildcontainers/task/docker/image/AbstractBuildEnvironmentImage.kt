package com.github.buildcontainers.task.docker.image

import com.github.buildcontainers.task.AbstractBuildContainersTask
import com.github.dockerjava.core.command.BuildImageResultCallback
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.io.File

abstract class AbstractBuildEnvironmentImage: AbstractBuildContainersTask() {
    @Input
    val intermediateEnvironmentImageTag: Property<String> = project.objects.property()

    @Internal
    val environmentImageTag: Property<String> = project.objects.property()

    @TaskAction
    fun buildEnvironmentImage() {
        val dockerfile = createDockerfile()
        dockerfile.writeText(buildDockerfileContent())
        val imageTag = environmentImageTag()
        runDockerCommand(dockerfile, imageTag)
        environmentImageTag.set(imageTag)
        logger.lifecycle("Environment image built with tag $imageTag")
    }

    private fun runDockerCommand(dockerfile: File, imageTag: String) {
        dockerClient.buildImageCmd(project.file(dockerfile))
                .withTags(setOf(imageTag))
                .exec(BuildImageResultCallback())
                .awaitImageId()
    }

    private fun createDockerfile(): File {
        project.mkdir("${project.buildDir}/buildcontainers/environment")
        val dockerfile = project.file("${project.buildDir}/buildcontainers/environment/Dockerfile")
        dockerfile.createNewFile()
        return dockerfile
    }

    private fun buildDockerfileContent(): String {
        return """
                FROM ${intermediateEnvironmentImageTag.get()}
                VOLUME /gradle-project-cache-dir
                VOLUME /gradle-user-home
                VOLUME /project-build-dir
                VOLUME /var/run/docker.sock
                WORKDIR /project-build-dir
                ${appendFileContent()}
            """.trimIndent()
    }

    private fun environmentImageTag(): String {
        return "${executionEnvironmentImageName()}:latest"
    }

    abstract fun appendFileContent(): String

    abstract fun executionEnvironmentImageName(): String

}