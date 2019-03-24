package com.github.buildcontainers.task

import com.github.buildcontainers.extension.docker.DockerConfiguration
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import groovy.transform.Internal
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.property

abstract class AbstractBuildContainersTask: DefaultTask() {

    @Nested
    val dockerConfiguration: Property<DockerConfiguration> = project.objects.property()

    @delegate:Internal
    val dockerClient: DockerClient by lazy(::buildDockerClient)

    private fun buildDockerClient(): DockerClient {
        val configuration = dockerConfiguration.getOrElse(DockerConfiguration(project))
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(configuration.dockerHost.get())
                .build()
        return DockerClientBuilder.getInstance(config).build()
    }

}