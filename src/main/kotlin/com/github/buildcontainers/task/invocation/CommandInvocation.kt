package com.github.buildcontainers.task.invocation

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty

open class CommandInvocation: AbstractBuildContainersInvocation() {
    @Input
    val commandList = project.objects.listProperty(String::class)

    @TaskAction
    fun runInvocation() {
        runCommand(commandList.get())
    }
}