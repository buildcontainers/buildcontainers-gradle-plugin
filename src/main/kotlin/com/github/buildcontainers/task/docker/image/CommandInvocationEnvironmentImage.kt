package com.github.buildcontainers.task.docker.image

import com.github.buildcontainers.naming.BuildContainersNamingConvention

open class CommandInvocationEnvironmentImage: AbstractBuildEnvironmentImage() {
    override fun executionEnvironmentImageName() = BuildContainersNamingConvention.commandInvocationImageName(project)
    override fun appendFileContent() = ""
}