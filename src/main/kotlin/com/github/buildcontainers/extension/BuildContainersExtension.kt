package com.github.buildcontainers.extension

import com.github.buildcontainers.extension.EnvironmentDefinitionType.BASE_IMAGE
import com.github.buildcontainers.extension.base_image.BaseImageEnvironmentConfiguration
import com.github.buildcontainers.extension.docker.DockerConfiguration
import com.github.buildcontainers.extension.dockerfile.DockerfileEnvironmentConfiguration
import com.github.buildcontainers.extension.environment.BuildEnvironmentConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class BuildContainersExtension(project: Project) {
    val environmentDefinitionType: Property<EnvironmentDefinitionType> = project.objects.property(EnvironmentDefinitionType::class).convention(BASE_IMAGE)

    val dockerfileEnvironmentConfiguration = DockerfileEnvironmentConfiguration(project)

    fun dockerfileEnvironment(action: Action<DockerfileEnvironmentConfiguration>) {
        action.execute(dockerfileEnvironmentConfiguration)
    }

    val baseImageEnvironmentConfiguration = BaseImageEnvironmentConfiguration(project)

    fun baseImageEnvironment(action: Action<BaseImageEnvironmentConfiguration>) {
        action.execute(baseImageEnvironmentConfiguration)
    }

    val buildEnvironmentConfiguration = BuildEnvironmentConfiguration(project)

    fun buildEnvironment(action: Action<BuildEnvironmentConfiguration>) {
        action.execute(buildEnvironmentConfiguration)
    }

    val dockerConfiguration = DockerConfiguration(project)

    fun docker(action: Action<DockerConfiguration>) {
        action.execute(dockerConfiguration)
    }
}