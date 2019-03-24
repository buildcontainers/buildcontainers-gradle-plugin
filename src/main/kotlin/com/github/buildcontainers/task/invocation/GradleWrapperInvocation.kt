package com.github.buildcontainers.task.invocation

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty

open class GradleWrapperInvocation: AbstractBuildContainersInvocation() {
    @Input
    var argumentList = project.objects.listProperty(String::class)

    @TaskAction
    fun runInvocation() {
        runCommand(argumentList.get())
    }
}