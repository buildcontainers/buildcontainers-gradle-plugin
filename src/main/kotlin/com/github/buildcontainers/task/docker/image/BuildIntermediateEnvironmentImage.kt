package com.github.buildcontainers.task.docker.image

import com.github.buildcontainers.extension.EnvironmentDefinitionType
import com.github.buildcontainers.extension.EnvironmentDefinitionType.BASE_IMAGE
import com.github.buildcontainers.naming.BuildContainersNamingConvention
import com.github.buildcontainers.task.AbstractBuildContainersTask
import com.github.dockerjava.core.command.BuildImageResultCallback
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import java.io.File

open class BuildIntermediateEnvironmentImage: AbstractBuildContainersTask() {

    @Input
    var environmentDefinitionType: Property<EnvironmentDefinitionType> = project.objects.property()

    @InputFile
    @Optional
    var dockerfile = project.objects.fileProperty()

    @InputDirectory
    @Optional
    var baseDirectory = project.objects.directoryProperty()

    @Input
    @Optional
    var baseImageName: Property<String> = project.objects.property()

    @Internal
    var intermediateEnvironmentImageTag: Property<String> = project.objects.property()

    @TaskAction
    fun buildIntermediateImage() {
        if (environmentDefinitionType.get() == BASE_IMAGE) {
            buildFromBaseImageDefinition()
        } else {
            buildFromDockerfileDefinition()
        }
    }

    private fun buildFromDockerfileDefinition() {
        val dockerfileValue = dockerfile.asFile.get()
        logger.lifecycle("Building intermediate environment image defined with Dockerfile ${dockerfileValue.path}")
        val tag = intermediateImageTag()
        runBuildImageCommand(dockerfileValue, tag)
        logger.lifecycle("Intermediate environment image defined with Dockerfile built with tag $tag")
        intermediateEnvironmentImageTag.set(tag)
    }

    private fun runBuildImageCommand(dockerfileValue: File, tag: String) {
        dockerClient.buildImageCmd(dockerfileValue)
                .withBaseDirectory(baseDirectory(dockerfileValue))
                .withTags(setOf(tag))
                .exec(BuildImageResultCallback())
                .awaitCompletion()
    }

    private fun intermediateImageTag(): String {
        val imageName = BuildContainersNamingConvention.intermediateEnvironmentImageName(project)
        return "$imageName:latest"
    }

    private fun baseDirectory(dockerfile: File): File {
        return if (baseDirectory.isPresent) {
            baseDirectory.asFile.get()
        } else {
            dockerfile.parentFile
        }
    }

    private fun buildFromBaseImageDefinition() {
        logger.lifecycle("Building intermediate environment image defined with base image ${baseImageName.get()}")
        intermediateEnvironmentImageTag.set(baseImageName)
    }
}
