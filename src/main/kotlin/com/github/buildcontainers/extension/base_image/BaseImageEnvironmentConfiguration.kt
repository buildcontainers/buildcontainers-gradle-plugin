package com.github.buildcontainers.extension.base_image

import org.gradle.api.Project
import org.gradle.kotlin.dsl.property

open class BaseImageEnvironmentConfiguration(project: Project) {
    val tag = project.objects.property(String::class).convention("openjdk:8-alpine")

    fun setTag(tag: String) {
        this.tag.set(tag)
    }
}