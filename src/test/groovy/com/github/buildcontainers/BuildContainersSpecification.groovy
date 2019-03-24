package com.github.buildcontainers

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class BuildContainersSpecification extends Specification {

    @Rule
    protected final TemporaryFolder projectDir = new TemporaryFolder()

    protected File buildFile
    protected File settingsFile

    def setup() {
        buildFile = projectDir.newFile("build.gradle")
        settingsFile = projectDir.newFile("settings.gradle")
        setupSettingsFile()
        setupBuildFile()
    }

    String rootProjectName() {
        return "buildcontainers-test-project"
    }

    def setupSettingsFile() {
        settingsFile << """
            rootProject.name='${rootProjectName()}'
        """
    }

    def setupBuildFile() {
        buildFile << """
            plugins {
                id 'base'
                id 'com.github.buildcontainers'
            }
        """
    }

    protected GradleRunner runner() {
        return GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withPluginClasspath()
                .forwardOutput()
    }

    protected GradleRunner withArguments(List<String> arguments) {
        return runner().withArguments(arguments)
    }

    protected GradleRunner withArguments(String... arguments) {
        return withArguments(argsList(arguments))
    }

    private static List<String> argsList(String... arguments) {
        List<String> list = arguments.toList()
        list.add("--stacktrace")
        return list
    }

    protected static String generateRandomUUID() {
        UUID.randomUUID().toString().replaceAll('-', '')
    }
}
