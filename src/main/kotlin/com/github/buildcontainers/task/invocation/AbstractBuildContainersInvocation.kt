package com.github.buildcontainers.task.invocation

import com.github.buildcontainers.extension.environment.BuildEnvironmentConfiguration
import com.github.buildcontainers.naming.BuildContainersNamingConvention
import com.github.buildcontainers.task.AbstractBuildContainersTask
import com.github.buildcontainers.task.docker.container.ContainerId
import com.github.buildcontainers.task.docker.container.LogContainerCallback
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.property
import java.io.File

abstract class AbstractBuildContainersInvocation: AbstractBuildContainersTask() {
    @Input
    val imageTag: Property<String> = project.objects.property()

    @Input
    @Optional
    val containerName: Property<String> = project.objects.property(String::class)
            .value(BuildContainersNamingConvention.buildEnvironmentContainerName(project, name))

    fun setContainerName(name: String) {
        containerName.set(name)
    }

    fun setContainerName(provider: Provider<String>) {
        containerName.set(provider)
    }

    @Nested
    val configuration: Property<BuildEnvironmentConfiguration> = project.objects.property()

    private fun getConfiguration(): BuildEnvironmentConfiguration {
        return configuration.get()
    }

    protected fun runCommand(command: List<String>) {
        val cleanUpThread = cleanupThread()
        try {
            addShutDownHook(cleanUpThread)
            executeCommandInsideContainer(command)
        }  catch (e: Exception) {
            logger.error("Build failed", e)
            throw GradleException("Build failed", e)
        } finally {
            removeShutdownHook(cleanUpThread)
        }
    }

    private fun executeCommandInsideContainer(command: List<String>) {
        removeExecutionContainer()
        val containerId = createContainer(command)
        startContainer(containerId)
        logContainer(containerId)
        check(containerId)
    }

    private fun addShutDownHook(cleanUpThread: Thread) {
        Runtime.getRuntime().addShutdownHook(cleanUpThread)
    }

    private fun removeShutdownHook(cleanUpThread: Thread) {
        Runtime.getRuntime().removeShutdownHook(cleanUpThread)
    }

    private fun cleanupThread(): Thread {
        return Thread({
            dockerClient.removeContainerCmd(containerName.get())
                    .withForce(true)
                    .withRemoveVolumes(true)
                    .exec()
        })
    }

    private fun createContainer(command: List<String>): ContainerId {
        val id = dockerClient.createContainerCmd(imageTag.get())
                .withHostConfig(hostConfig())
                .withCmd(command)
                .withName(containerName.get())
                .withUser(user())
                .exec()
                .id
        return ContainerId(id)
    }

    private fun hostConfig(): HostConfig? {
        return HostConfig.newHostConfig()
                    .withBinds(binds())
                    .withGroupAdd(configuration.get().groups.get())
    }

    private fun binds(): List<Bind> {
        val binds = mutableListOf(
                projectBuildBind(),
                gradleUserHomeBind(),
                gradleProjectCacheDirBind())

        if (getConfiguration().dockerSocketPath.isPresent) {
            binds.add(buildBind("/var/run/docker.sock", getConfiguration().dockerSocketPath))
        }

        return binds
    }

    private fun projectBuildBind() = Bind(project.rootDir.absolutePath, Volume("/project-build-dir"))

    private fun gradleUserHomeBind() = buildBind("/gradle-user-home", getConfiguration().gradleUserHome)

    private fun gradleProjectCacheDirBind() = buildBind("/gradle-project-cache-dir", getConfiguration().gradleProjectCacheDir)

    private fun buildBind(path: String, volume: Property<String>): Bind {
        createDir(volume)
        return Bind(volume.get(), Volume(path))
    }

    private fun createDir(volume: Property<String>) {
        val volumeFile = File(volume.get())
        if (!volumeFile.exists()) {
            if (!volumeFile.mkdirs()) {
                throw GradleException("Cannot create file ${volume.get()}")
            }
        }
    }

    private fun startContainer(id: ContainerId) {
        dockerClient.startContainerCmd(id.id).exec()
    }

    private fun logContainer(id: ContainerId) {
        dockerClient.logContainerCmd(id.id)
                .withFollowStream(true)
                .withStdErr(true)
                .withStdOut(true)
                .withTimestamps(getConfiguration().displayTimestamps.get())
                .withTailAll()
                .exec(LogContainerCallback { msg -> logger.lifecycle(msg)})
                .awaitCompletion()
    }

    private fun check(id: ContainerId) {
        val exitCode = dockerClient.inspectContainerCmd(id.id)
                .exec()
                .state.exitCode

        if (exitCode != 0) {
            throw GradleException("Invocation finished with error exit code $exitCode")
        }
    }

    private fun user(): String {
        val uid = getConfiguration().userId.get()
        val guid = getConfiguration().groupId.get()
        return "$uid:$guid"
    }

    private fun removeExecutionContainer(): Boolean {
        return try {
            dockerClient.removeContainerCmd(containerName.get())
                    .withRemoveVolumes(true)
                    .withForce(true)
                    .exec()
            true
        } catch (ex: NotFoundException) {
            false
        }
    }
}