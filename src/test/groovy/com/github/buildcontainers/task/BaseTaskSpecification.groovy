package com.github.buildcontainers.task

import com.github.buildcontainers.BuildContainersSpecification
import com.github.buildcontainers.docker.DockerTestCommands
import com.github.buildcontainers.naming.BuildContainersNamingConvention
import org.gradle.testkit.runner.BuildResult

import static com.github.buildcontainers.BuildContainersPlugin.BUILD_COMMAND_INVOCATION_ENV_TASK
import static com.github.buildcontainers.BuildContainersPlugin.BUILD_GRADLE_WRAPPER_INVOCATION_ENV_TASK
import static com.github.buildcontainers.BuildContainersPlugin.BUILD_INTERMEDIATE_ENV_TASK

abstract class BaseTaskSpecification extends BuildContainersSpecification {

    public static final String INTERMEDIATE_ENV_TASK = ":$BUILD_INTERMEDIATE_ENV_TASK"
    public static final String COMMAND_INVOCATION_ENV_TASK = ":$BUILD_COMMAND_INVOCATION_ENV_TASK"
    public static final String GRADLE_WRAPPER_INVOCATION_ENV_TASK = ":$BUILD_GRADLE_WRAPPER_INVOCATION_ENV_TASK"
    private static final String SUFFIX = "latest"
    protected String randomProjectUUID = generateRandomUUID()

    protected String intermediateEnvironmentImageNameTag() {
        return "${BuildContainersNamingConvention.intermediateEnvironmentImageName(rootProjectName())}:$SUFFIX"
    }

    protected String gradleWrapperImageTag() {
        return "${BuildContainersNamingConvention.gradleWrapperInvocationImageName(rootProjectName())}:$SUFFIX"
    }

    protected String commandImageTag() {
        return "${BuildContainersNamingConvention.commandInvocationImageName(rootProjectName())}:$SUFFIX"
    }

    protected String environmentContainerName(String taskName = "invoke") {
        return BuildContainersNamingConvention.buildEnvironmentContainerName(rootProjectName(), taskName)
    }

    protected String removeContainerTaskName(String invocationTaskName = "invoke") {
        return ":" + BuildContainersNamingConvention.removeEnvironmentContainerTaskName(invocationTaskName)
    }

    def cleanup() {
        DockerTestCommands.removeImage(intermediateEnvironmentImageNameTag())
        DockerTestCommands.removeImage(commandImageTag())
        DockerTestCommands.removeImage(gradleWrapperImageTag())
    }

    protected String getOutput(BuildResult result) {
        return result.output.replace("/private/", "/")
    }
}
