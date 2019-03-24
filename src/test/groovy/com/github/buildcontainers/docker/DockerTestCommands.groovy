package com.github.buildcontainers.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PullImageResultCallback
import groovy.transform.Memoized

class DockerTestCommands {

    static void runContainerWithName(String containerName) {
        dockerTestClient().pullImageCmd("alpine")
                .withTag("3.4")
                .exec(new PullImageResultCallback()).awaitCompletion()
        def containerId = dockerTestClient().createContainerCmd("alpine:3.4")
                .withName(containerName)
                .withCmd("sleep", "9999999")
                .exec().id
        dockerTestClient().startContainerCmd(containerId).exec()
    }

    static boolean containerIsRunning(String containerName) {
        try {
            dockerTestClient().inspectContainerCmd(containerName)
                    .exec()
                    .state.running
        } catch (NotFoundException ignored) {
            return false
        }
    }

    static boolean containerExists(String containerName) {
        try {
            dockerTestClient().inspectContainerCmd(containerName).exec()
            return true
        } catch (NotFoundException ignored) {
            return false
        }
    }

    static void removeContainer(String containerName) {
        try {
            dockerTestClient().removeContainerCmd(containerName)
                    .withForce(true)
                    .withRemoveVolumes(true)
                    .exec()
        } catch (NotFoundException ignored) {
            // nothing to do here
        }
    }

    static boolean imageExists(String tag) {
        try {
            dockerTestClient().inspectImageCmd(tag).exec()
            return true
        } catch (NotFoundException ignored) {
            return false
        }
    }

    static def removeImage(String tag) {
        try {
            dockerTestClient().removeImageCmd(tag.toString()).exec()
        } catch (NotFoundException ignored) {
            // nothing to do here
        }
    }

    @Memoized
    private static DockerClient dockerTestClient() {
        def config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build()
        return DockerClientBuilder.getInstance(config).build()
    }

}
