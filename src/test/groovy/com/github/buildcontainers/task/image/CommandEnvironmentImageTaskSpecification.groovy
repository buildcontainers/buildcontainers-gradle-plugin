package com.github.buildcontainers.task.image

import com.github.buildcontainers.docker.DockerTestCommands
import com.github.buildcontainers.task.BaseTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class CommandEnvironmentImageTaskSpecification extends BaseTaskSpecification {

    @Override
    String rootProjectName() {
        return "ceit-test-proj-${randomProjectUUID}"
    }

    def "should build environment image defined with dockerfile"() {
        given:
        projectDir.newFolder("custom-docker")
        File dockerfile = projectDir.newFile("custom-docker/CoolDockerfile")
        dockerfile << """
            FROM openjdk:11-oracle
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.CommandInvocation

            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
                
                dockerfileEnvironment {
                    dockerfile = file('custom-docker/CoolDockerfile')
                }
            }
            
            task invoke(type: CommandInvocation) {
                commandList = [ "java", "-version" ]
            }
        """

        expect:
        !DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())
        !DockerTestCommands.imageExists(commandImageTag())

        when:
        BuildResult result = withArguments(COMMAND_INVOCATION_ENV_TASK).build()

        then:
        result.task(INTERMEDIATE_ENV_TASK).outcome == TaskOutcome.SUCCESS
        String output = getOutput(result)
        output.contains("Building intermediate environment image defined with Dockerfile ${dockerfile.absolutePath}")
        output.contains("Intermediate environment image defined with Dockerfile built with tag ${intermediateEnvironmentImageNameTag()}")
        DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())

        result.task(GRADLE_WRAPPER_INVOCATION_ENV_TASK) == null
        result.task(COMMAND_INVOCATION_ENV_TASK).outcome == TaskOutcome.SUCCESS
        output.contains("Environment image built with tag ${commandImageTag()}")
        DockerTestCommands.imageExists(commandImageTag())
    }


    def "should fail when dockerfile extension property not set"() {
        given:
        projectDir.newFolder("custom-docker")
        File dockerfile = projectDir.newFile("custom-docker/CoolDockerfile")
        dockerfile << """
            FROM openjdk:8-slim
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.CommandInvocation

            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
            }
            
            task invoke(type: CommandInvocation) {
                commandList = [ "java", "-version" ]
            }
        """

        expect:
        !DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())
        !DockerTestCommands.imageExists(commandImageTag())

        when:
        BuildResult result = withArguments("invoke").buildAndFail()

        then:
        result.task(INTERMEDIATE_ENV_TASK).outcome == TaskOutcome.FAILED

        result.task(GRADLE_WRAPPER_INVOCATION_ENV_TASK) == null
        result.task(COMMAND_INVOCATION_ENV_TASK) == null
        result.task(":invoke") == null

        !DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())
        !DockerTestCommands.imageExists(commandImageTag())
    }

    def "should build environment image defined with base image"() {
        given:
        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.CommandInvocation
            
            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.BASE_IMAGE
                
                baseImageEnvironment {
                    tag = "openjdk:8-slim"
                }
            }
            
            task invoke(type: CommandInvocation) {
                commandList = [ "java", "-version" ]
            }
        """

        expect:
        !DockerTestCommands.imageExists(commandImageTag())

        when:
        BuildResult result = withArguments(COMMAND_INVOCATION_ENV_TASK).build()

        then:
        result.task(INTERMEDIATE_ENV_TASK).outcome == TaskOutcome.SUCCESS
        result.output.contains("Building intermediate environment image defined with base image openjdk:8-slim")

        result.task(GRADLE_WRAPPER_INVOCATION_ENV_TASK) == null
        result.task(COMMAND_INVOCATION_ENV_TASK).outcome == TaskOutcome.SUCCESS
        result.output.contains("Environment image built with tag ${commandImageTag()}")
        DockerTestCommands.imageExists(commandImageTag())
    }
}
