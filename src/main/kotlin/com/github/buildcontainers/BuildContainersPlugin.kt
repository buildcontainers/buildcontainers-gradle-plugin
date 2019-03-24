package com.github.buildcontainers

import com.github.buildcontainers.extension.BuildContainersExtension
import com.github.buildcontainers.naming.BuildContainersNamingConvention
import com.github.buildcontainers.task.AbstractBuildContainersTask
import com.github.buildcontainers.task.docker.container.RemoveEnvironmentContainer
import com.github.buildcontainers.task.docker.image.BuildGradleWrapperEnvironmentImage
import com.github.buildcontainers.task.docker.image.BuildIntermediateEnvironmentImage
import com.github.buildcontainers.task.docker.image.CommandInvocationEnvironmentImage
import com.github.buildcontainers.task.invocation.AbstractBuildContainersInvocation
import com.github.buildcontainers.task.invocation.CommandInvocation
import com.github.buildcontainers.task.invocation.GradleWrapperInvocation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

open class BuildContainersPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME = "buildcontainers"
        const val BUILD_INTERMEDIATE_ENV_TASK = "buildIntermediateEnvironmentImage"
        const val BUILD_GRADLE_WRAPPER_INVOCATION_ENV_TASK = "buildGradleWrapperInvocationEnvironmentImage"
        const val BUILD_COMMAND_INVOCATION_ENV_TASK = "buildCommandInvocationEnvironmentImage"
    }

    override fun apply(project: Project) {
        val pluginExtension = project.extensions.create(EXTENSION_NAME, BuildContainersExtension::class, project)
        val buildIntermediateEnvTask = prepareBuildIntermediateEnvTask(project, pluginExtension)

        prepareGradleWrapperInvocation(project, buildIntermediateEnvTask, pluginExtension)
        prepareCmdInvocation(project, buildIntermediateEnvTask, pluginExtension)
        prepareBuildContainerTask(project, pluginExtension)
    }

    private fun prepareCmdInvocation(project: Project,
                                     buildIntermediateEnv: TaskProvider<BuildIntermediateEnvironmentImage>,
                                     pluginExtension: BuildContainersExtension) {
        val buildCommandEnvTask = prepareCommandInvocationEnvTask(project, buildIntermediateEnv)

        project.tasks.withType<CommandInvocation> {
            dependsOn(buildCommandEnvTask)
            imageTag.set(buildCommandEnvTask.get().environmentImageTag)
            configuration.set(pluginExtension.buildEnvironmentConfiguration)
            finalizedBy(prepareRemoveEnvContainerTask(project, this))
        }
    }

    private fun prepareGradleWrapperInvocation(project: Project,
                                               buildIntermediateEnv: TaskProvider<BuildIntermediateEnvironmentImage>,
                                               pluginExtension: BuildContainersExtension) {
        val buildGWEnvTask = prepareGradleWrapperInvocationEnvTask(project, buildIntermediateEnv)

        project.tasks.withType<GradleWrapperInvocation> {
            dependsOn(buildGWEnvTask)
            imageTag.set(buildGWEnvTask.get().environmentImageTag)
            configuration.set(pluginExtension.buildEnvironmentConfiguration)
            finalizedBy(prepareRemoveEnvContainerTask(project, this))
        }
    }

    private fun prepareRemoveEnvContainerTask(project: Project, invocationTask: AbstractBuildContainersInvocation): TaskProvider<RemoveEnvironmentContainer> {
        val name = BuildContainersNamingConvention.removeEnvironmentContainerTaskName(invocationTask.name)
        return project.tasks.register(name, RemoveEnvironmentContainer::class, {
            containerName.set(invocationTask.containerName)
        })
    }

    private fun prepareBuildContainerTask(project: Project, pluginExtension: BuildContainersExtension) {
        project.tasks.withType<AbstractBuildContainersTask> {
            dockerConfiguration.set(pluginExtension.dockerConfiguration)
        }
    }

    private fun prepareCommandInvocationEnvTask(project: Project,
                                                buildIntermediateEnv: TaskProvider<BuildIntermediateEnvironmentImage>): TaskProvider<CommandInvocationEnvironmentImage> {
        return project.tasks.register(BUILD_COMMAND_INVOCATION_ENV_TASK, CommandInvocationEnvironmentImage::class, {
            dependsOn(buildIntermediateEnv)
            intermediateEnvironmentImageTag.set(buildIntermediateEnv.get().intermediateEnvironmentImageTag)
        })
    }

    private fun prepareGradleWrapperInvocationEnvTask(project: Project,
                                                      buildIntermediateEnv: TaskProvider<BuildIntermediateEnvironmentImage>): TaskProvider<BuildGradleWrapperEnvironmentImage> {
        return project.tasks.register(BUILD_GRADLE_WRAPPER_INVOCATION_ENV_TASK, BuildGradleWrapperEnvironmentImage::class, {
            dependsOn(buildIntermediateEnv)
            intermediateEnvironmentImageTag.set(buildIntermediateEnv.get().intermediateEnvironmentImageTag)
        })
    }

    private fun prepareBuildIntermediateEnvTask(project: Project,
                                                pluginExtension: BuildContainersExtension): TaskProvider<BuildIntermediateEnvironmentImage> {
        return project.tasks.register(BUILD_INTERMEDIATE_ENV_TASK, BuildIntermediateEnvironmentImage::class, {
            environmentDefinitionType.set(pluginExtension.environmentDefinitionType)
            baseImageName.set(pluginExtension.baseImageEnvironmentConfiguration.tag)
            dockerfile.set(pluginExtension.dockerfileEnvironmentConfiguration.dockerfile)
            baseDirectory.set(pluginExtension.dockerfileEnvironmentConfiguration.baseDirectory)
        })
    }
}
