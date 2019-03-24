package com.github.buildcontainers.task.docker.container

import com.github.buildcontainers.task.AbstractBuildContainersTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class RemoveEnvironmentContainer: AbstractBuildContainersTask() {
    @Input
    val containerName = project.objects.property(String::class)

    @TaskAction
    fun removeContainer() {
        dockerClient.removeContainerCmd(containerName.get())
                .withRemoveVolumes(true)
                .withForce(true)
                .exec()
    }
}