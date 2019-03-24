package com.github.buildcontainers.extension.dockerfile

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import java.io.File

open class DockerfileEnvironmentConfiguration(project: Project) {
    val dockerfile = project.objects.fileProperty()

    val baseDirectory = project.objects.directoryProperty()

    val noCache: Property<Boolean> = project.objects.property(Boolean::class).value(false)

    fun setDockerfile(dockerfile: File) {
        this.dockerfile.set(dockerfile)
    }

    fun setBaseDirectory(baseDirectory: File) {
        this.baseDirectory.set(baseDirectory)
    }

    fun setNoCache(noCache: Boolean) {
        this.noCache.set(noCache)
    }

    init {
        baseDirectory.dir(dockerfile.map{ it.asFile.parent })
    }
}