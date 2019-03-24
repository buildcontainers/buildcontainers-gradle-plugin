package com.github.buildcontainers.extension.docker

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property

open class DockerConfiguration(project: Project) {
    @Input
    val dockerHost = project.objects.property(String::class).value("unix:///var/run/docker.sock")

    fun setDockerHost(dockerHost: String) {
        this.dockerHost.set(dockerHost)
    }
}
