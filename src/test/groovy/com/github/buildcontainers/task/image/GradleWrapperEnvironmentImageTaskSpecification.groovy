package com.github.buildcontainers.task.image


import com.github.buildcontainers.docker.DockerTestCommands
import com.github.buildcontainers.task.BaseTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class GradleWrapperEnvironmentImageTaskSpecification extends BaseTaskSpecification {

    @Override
    String rootProjectName() {
        return "gweit-test-proj-${randomProjectUUID}"
    }

    def "should build environment image defined with dockerfile"() {
        given:
        projectDir.newFolder("another-docker")
        File dockerfile = projectDir.newFile("another-docker/CustomDockerfile")
        dockerfile << """
            FROM openjdk:8-slim
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
                
                dockerfileEnvironment {
                    dockerfile = file('another-docker/CustomDockerfile')
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "tasks" ]
            }
        """

        expect:
        !DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())
        !DockerTestCommands.imageExists(gradleWrapperImageTag())

        when:
        BuildResult result = withArguments(GRADLE_WRAPPER_INVOCATION_ENV_TASK).build()

        then:
        result.task(INTERMEDIATE_ENV_TASK).outcome == TaskOutcome.SUCCESS
        String output = getOutput(result)
        output.contains("Building intermediate environment image defined with Dockerfile ${dockerfile.absolutePath}")
        output.contains("Intermediate environment image defined with Dockerfile built with tag ${intermediateEnvironmentImageNameTag()}")
        DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())

        result.task(COMMAND_INVOCATION_ENV_TASK) == null
        result.task(GRADLE_WRAPPER_INVOCATION_ENV_TASK).outcome == TaskOutcome.SUCCESS
        output.contains("Environment image built with tag ${gradleWrapperImageTag()}")
        DockerTestCommands.imageExists(gradleWrapperImageTag())
    }

    def "should fail when dockerfile extension property not set"() {
        given:
        projectDir.newFolder("another-docker")
        File dockerfile = projectDir.newFile("another-docker/CustomDockerfile")
        dockerfile << """
            FROM openjdk:8-slim
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "tasks" ]
            }
        """

        expect:
        !DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())
        !DockerTestCommands.imageExists(gradleWrapperImageTag())

        when:
        BuildResult result = withArguments("invoke").buildAndFail()

        then:
        result.task(INTERMEDIATE_ENV_TASK).outcome == TaskOutcome.FAILED

        result.task(COMMAND_INVOCATION_ENV_TASK) == null
        result.task(GRADLE_WRAPPER_INVOCATION_ENV_TASK) == null
        result.task(":invoke") == null

        !DockerTestCommands.imageExists(intermediateEnvironmentImageNameTag())
        !DockerTestCommands.imageExists(gradleWrapperImageTag())
    }

    def "should build environment image defined with base image"() {
        given:
        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation
            
            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.BASE_IMAGE
                
                baseImageEnvironment {
                    tag = "alpine:3.4"
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "tasks" ]
            }
        """

        expect:
        !DockerTestCommands.imageExists(gradleWrapperImageTag())

        when:
        BuildResult result = withArguments(GRADLE_WRAPPER_INVOCATION_ENV_TASK).build()

        then:
        result.task(INTERMEDIATE_ENV_TASK).outcome == TaskOutcome.SUCCESS
        result.output.contains("Building intermediate environment image defined with base image alpine:3.4")

        result.task(COMMAND_INVOCATION_ENV_TASK) == null
        result.task(GRADLE_WRAPPER_INVOCATION_ENV_TASK).outcome == TaskOutcome.SUCCESS
        result.output.contains("Environment image built with tag ${gradleWrapperImageTag()}")
        DockerTestCommands.imageExists(gradleWrapperImageTag())
    }
}
