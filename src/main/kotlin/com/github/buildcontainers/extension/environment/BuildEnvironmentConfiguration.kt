package com.github.buildcontainers.extension.environment

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.util.concurrent.Callable

open class BuildEnvironmentConfiguration(project: Project) {
    @Input
    val userId = project.objects.property(String::class)
            .convention(project.provider(executeCommand("id", "-u")))

    fun setUserId(userId: String) {
        this.userId.set(userId)
    }

    fun setUserId(provider: Provider<String>) {
        this.userId.set(provider)
    }

    @Input
    @Optional
    val groupId: Property<String> = project.objects.property(String::class)
            .convention(project.provider(groupIdConvention()))


    fun setGroupId(groupId: String) {
        this.groupId.set(groupId)
    }

    fun setGroupId(provider: Provider<String>) {
        this.groupId.set(provider)
    }

    @Input
    @Optional
    val groups: ListProperty<String> = project.objects.listProperty(String::class)
            .convention(project.provider(groups()))

    private fun groups(): Callable<List<String>> {
        return Callable {
            runCommand("id", "-G")?.split(" ")
        }
    }

    fun setGroups(groups: List<String>) {
        this.groups.set(groups)
    }

    fun setGroups(groups: ListProperty<String>) {
        this.groups.set(groups)
    }

    @Input
    @Optional
    val dockerSocketPath: Property<String> = project.objects.property()

    fun setDockerSocketPath(path: String) {
        dockerSocketPath.set(path)
    }

    @Input
    val gradleUserHome = project.objects.property(String::class)
            .convention(project.provider(buildContainersDirectory(project, "gradle/user-home")))

    fun setGradleUserHome(path: String) {
        this.gradleUserHome.set(path)
    }

    @Input
    val gradleProjectCacheDir = project.objects.property(String::class)
            .convention(project.provider(buildContainersDirectory(project, "gradle/project-cache-dir")))

    fun setGradleProjectCacheDir(path: String) {
        this.gradleProjectCacheDir.set(path)
    }

    @Input
    val displayTimestamps = project.objects.property(Boolean::class).convention(false)

    private fun groupIdConvention(): Callable<String> {
        return Callable {
            if (Os.isFamily(Os.FAMILY_MAC)) {
                "0"
            } else {
                runCommand("id", "-g")
            }
        }
    }

    private fun executeCommand(vararg commandElements: String): Callable<String> {
        return Callable {
            runCommand(*commandElements)
        }
    }

    private fun runCommand(vararg commandElements: String): String? {
        val process = ProcessBuilder(*commandElements)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
        process.waitFor()
        return if (process.exitValue() == 0) {
            process.inputStream.bufferedReader().readText().trim()
        } else {
            null
        }
    }

    private fun buildContainersDirectory(project: Project, directory: String): Callable<String> {
        return Callable {
            "${System.getProperty("user.home")}/.buildcontainers/${project.name}/$directory"
        }
    }
}