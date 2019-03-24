package com.github.buildcontainers.task.invocation

import com.github.buildcontainers.docker.DockerTestCommands
import com.github.buildcontainers.task.BaseTaskSpecification
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class GradleWrapperInvocationSpecification extends BaseTaskSpecification {

    private File internalBuildFile

    def setup() {
        internalBuildFile = projectDir.newFile("internalBuild.gradle")
    }

    @Override
    String rootProjectName() {
        return "gwi-test-proj-${randomProjectUUID}"
    }

    void copyGradleWrapper(String version = "5.2.1") {
        FileUtils.copyDirectory(new File("src/test/resources/wrapper/$version"), projectDir.root)
        new File("${projectDir.root}/gradlew").setExecutable(true)
    }

    def "should invoke gradle inside container defined with dockerfile"() {
        given:
        copyGradleWrapper()
        projectDir.newFolder("gwi-docker", "resources")
        projectDir.newFile("gwi-docker/resources/boring-file.txt") << """
            Gradle wrapper file content
        """

        projectDir.newFile("gwi-docker/SomeDockerfile") << """
            FROM openjdk:11-oracle
            COPY resources/boring-file.txt /tmp/boring-file.txt
        """

        internalBuildFile << """
            task readBoringFileContent() {
                doLast {
                    def content = new File('/tmp/boring-file.txt').text
                    println content
                }
            }
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation
            
            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
                
                dockerfileEnvironment {
                    dockerfile = file('gwi-docker/SomeDockerfile')
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "readBoringFileContent", "--build-file", "internalBuild.gradle" ]
            }
        """

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("Gradle wrapper file content")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "should invoke gradle inside container defined with base image"() {
        given:
        copyGradleWrapper()
        internalBuildFile << """
            task printSomeContent() {
                doLast {
                    println "Boring content which should be printed"
                }
            }
        """

        buildFile << """
            import com.github.buildcontainers.extension.EnvironmentDefinitionType
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation
            
            buildcontainers {
                environmentDefinitionType = EnvironmentDefinitionType.BASE_IMAGE
                
                baseImageEnvironment {
                    tag = "openjdk:8-slim"
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "printSomeContent", "--build-file", "internalBuild.gradle" ]
            }
        """

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("Boring content which should be printed")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "gradle wrapper invocation inside container should fail"() {
        given:
        copyGradleWrapper()
        buildFile << """
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

            buildcontainers {
                baseImageEnvironment {
                    tag = "openjdk:8-slim"
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "invalidTask", "--build-file", "internalBuild.gradle" ]
            }
        """

        expect:
        !DockerTestCommands.containerExists(environmentContainerName())

        when:
        BuildResult result = withArguments("invoke").buildAndFail()

        then:
        result.output.contains("FAILURE: Build failed with an exception.")
        result.output.contains("Task 'invalidTask' not found in root project")
        result.task(":invoke").outcome == TaskOutcome.FAILED
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "should remove existing container from previous build"() {
        given:
        copyGradleWrapper()
        internalBuildFile << """
            task printSomeContent() {
                doLast {
                    println "Cool content which should be printed"
                }
            }
        """

        buildFile << """
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

            buildcontainers {
                baseImageEnvironment {
                    tag = "openjdk:8-slim"
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "printSomeContent", "--build-file", "internalBuild.gradle" ]
            }
        """
        DockerTestCommands.runContainerWithName(environmentContainerName())

        expect:
        DockerTestCommands.containerIsRunning(environmentContainerName())

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("Cool content which should be printed")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "should mount docker sock inside container"() {
        given:
        copyGradleWrapper()
        internalBuildFile << """
            plugins {
                id "com.bmuschko.docker-remote-api" version "4.5.0"
            }

            import com.bmuschko.gradle.docker.tasks.container.DockerInspectContainer

            task inspectBuildEnvContainer(type: DockerInspectContainer) {
                containerId = "${environmentContainerName()}"
            }
        """

        buildFile << """
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

            buildcontainers {
                baseImageEnvironment {
                    tag = "openjdk:8-slim"
                }
                
                buildEnvironment {
                    dockerSocketPath = "/var/run/docker.sock"
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "inspectBuildEnvContainer", "--build-file", "internalBuild.gradle" ]
            }
        """
        DockerTestCommands.runContainerWithName(environmentContainerName())

        expect:
        DockerTestCommands.containerIsRunning(environmentContainerName())

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.output.contains("Inspecting container with ID '${environmentContainerName()}'.")
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.task(removeContainerTaskName()).outcome == TaskOutcome.SUCCESS
        !DockerTestCommands.containerExists(environmentContainerName())
    }

    def "should mount gradle directories inside container"() {
        given:
        copyGradleWrapper("5.2.1")
        internalBuildFile << """
            task printMsg {
                doLast {
                    println "Hello world!"
                }
            }
        """

        buildFile << """
            import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

            buildcontainers {
                baseImageEnvironment {
                    tag = "openjdk:8-slim"
                }
                
                buildEnvironment {
                    dockerSocketPath = "/var/run/docker.sock"
                    gradleUserHome = file(".gradle-docker/user-home")
                    gradleProjectCacheDir = file(".gradle-docker/project-cache")
                }
            }
            
            task invoke(type: GradleWrapperInvocation) {
                argumentList = [ "printMsg", "--build-file", "internalBuild.gradle" ]
            }
        """

        when:
        BuildResult result = withArguments("invoke").build()

        then:
        result.task(":invoke").outcome == TaskOutcome.SUCCESS
        result.output.contains("Hello world!")
        result.output.contains("Downloading https://services.gradle.org/distributions/gradle-5.2.1-bin.zip")

        when:
        BuildResult secondInvocation = withArguments("clean", "invoke").build()

        then:
        secondInvocation.task(":invoke").outcome == TaskOutcome.SUCCESS
        secondInvocation.output.contains("Hello world!")
        !secondInvocation.output.contains("Downloading https://services.gradle.org/distributions/gradle-5.2.1-bin.zip")
    }
}
