package com.github.buildcontainers.task.invocation

import com.github.buildcontainers.docker.DockerTestCommands
import com.github.buildcontainers.task.BaseTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class CommandInvocationSpecification extends BaseTaskSpecification {

    @Override
    String rootProjectName() {
        return "ci-test-proj-${randomProjectUUID}"
    }

    def "should run command inside container defined with dockerfile"() {
        given:
        projectDir.newFolder("my-docker", "resources")
        projectDir.newFile("my-docker/resources/boring-file.txt") << """
            Boring file content
        """
        projectDir.newFile("my-docker/CoolDockerfile") << """
            FROM openjdk:11-oracle
            COPY resources/boring-file.txt /tmp/boring-file.txt
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.CommandInvocation

            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
                
                dockerfileEnvironment {
                    dockerfile = file('my-docker/CoolDockerfile')
                }
            }
            
            task invoke(type: CommandInvocation) {
                commandList = [ "cat", "/tmp/boring-file.txt" ]
            }
        """

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("Boring file content")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "should run command inside container defined with base image"() {
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
                commandList = [ "echo", "'Echoed inside build container'" ]
            }
        """

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("Echoed inside build container")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "command invocation inside container should fail gradle invocation"() {
        given:
        projectDir.newFolder("my-docker")
        projectDir.newFile("my-docker/CoolDockerfile") << """
            FROM openjdk:11-oracle
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.CommandInvocation

            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
                
                dockerfileEnvironment {
                    dockerfile = file('my-docker/CoolDockerfile')
                }
            }
            
            task invoke(type: CommandInvocation) {
                commandList = [ "ls", "/tmp/boring-file.txt" ]
            }
        """

        expect:
        !DockerTestCommands.containerExists(environmentContainerName())

        when:
        BuildResult result = withArguments("invoke").buildAndFail()

        then:
        result.output.contains("ls: cannot access /tmp/boring-file.txt: No such file or directory")
        result.output.contains("Invocation finished with error exit code 2")
        result.task(":invoke").outcome == TaskOutcome.FAILED
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "should remove existing container from previous build"() {
        given:
        projectDir.newFolder("boring-docker", "resources")
        projectDir.newFile("boring-docker/resources/boring-file.txt") << """
            File content
        """
        projectDir.newFile("boring-docker/Dockerfile") << """
            FROM openjdk:11-oracle
            COPY resources/boring-file.txt /tmp/boring-file.txt
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.CommandInvocation

            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
                
                dockerfileEnvironment {
                    dockerfile = file('boring-docker/Dockerfile')
                }
            }
            
            task invoke(type: CommandInvocation) {
                commandList = [ "cat", "/tmp/boring-file.txt" ]
            }
        """
        DockerTestCommands.runContainerWithName(environmentContainerName())

        expect:
        DockerTestCommands.containerIsRunning(environmentContainerName())

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("File content")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "should mount docker sock inside container"() {
        given:
        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.CommandInvocation

            buildcontainers {
                baseImageEnvironment {
                    tag = "docker:17.06.2-ce"
                }

                buildEnvironment {
                    dockerSocketPath = "/var/run/docker.sock"
                }
            }
            
            task invoke(type: CommandInvocation) {
                commandList = [ "docker", "inspect", "${environmentContainerName()}" ]
            }
        """

        expect:
        !DockerTestCommands.containerExists(environmentContainerName())

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("\"Image\": \"${commandImageTag()}\"")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

}
