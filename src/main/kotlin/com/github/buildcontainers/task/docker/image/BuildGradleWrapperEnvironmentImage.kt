package com.github.buildcontainers.task.docker.image

import com.github.buildcontainers.naming.BuildContainersNamingConvention

open class BuildGradleWrapperEnvironmentImage: AbstractBuildEnvironmentImage() {
    override fun executionEnvironmentImageName() = BuildContainersNamingConvention.gradleWrapperInvocationImageName(project)

    override fun appendFileContent(): String {
        return """
            ENTRYPOINT ["./gradlew", \
                        "--no-daemon", \
                        "--stacktrace", \
                        "--gradle-user-home=/gradle-user-home", \
                        "--project-cache-dir=/gradle-project-cache-dir" \
                        ]
        """.trimIndent()
    }
}