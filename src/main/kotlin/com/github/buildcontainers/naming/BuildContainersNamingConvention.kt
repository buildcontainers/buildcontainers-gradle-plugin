package com.github.buildcontainers.naming

import org.gradle.api.Project

class BuildContainersNamingConvention {

    companion object {
        fun intermediateEnvironmentImageName(project: Project): String {
            return intermediateEnvironmentImageName(projectName(project))
        }

        @JvmStatic
        fun intermediateEnvironmentImageName(projectName: String): String {
            return "$projectName/intermediate-environment-image"
        }

        fun commandInvocationImageName(project: Project): String {
            return commandInvocationImageName(projectName(project))
        }

        @JvmStatic
        fun commandInvocationImageName(projectName: String): String {
            return "$projectName/command-invocation"
        }

        fun gradleWrapperInvocationImageName(project: Project): String {
            return gradleWrapperInvocationImageName(projectName(project))
        }

        @JvmStatic
        fun gradleWrapperInvocationImageName(projectName: String): String {
            return "$projectName/gradle-wrapper-invocation"
        }

        fun buildEnvironmentContainerName(project: Project, taskName: String): String {
            return buildEnvironmentContainerName(projectName(project, "_"), taskName)
        }

        @JvmStatic
        fun buildEnvironmentContainerName(projectName: String, taskName: String = ""): String {
            return "${projectName}-${taskName}_buildcontainers-environment-container"
        }

        @JvmStatic
        fun removeEnvironmentContainerTaskName(invocationTaskName: String): String {
            return "${invocationTaskName}_remove-container"
        }

        private fun projectName(project: Project, delimiter: String = "/"): String {
            return if (project.rootProject == project) {
                project.name
            } else {
                "${project.rootProject.name}$delimiter${normalizeProjectPath(project, delimiter)}"
            }
        }

        private fun normalizeProjectPath(project: Project, delimiter: String = "/"): String {
            return project.path.trim(':').replace(":", delimiter)
        }
    }

}