import com.github.buildcontainers.extension.BuildContainersExtension
import com.github.buildcontainers.extension.EnvironmentDefinitionType
import com.github.buildcontainers.task.invocation.CommandInvocation

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.3.10"))
    }
}

repositories {
    jcenter()
}

plugins {
    id("com.github.buildcontainers") version "0.1.0"
}

buildcontainers {
    environmentDefinitionType.set(EnvironmentDefinitionType.BASE_IMAGE)

    baseImageEnvironment {
        tag.set("openjdk:9-slim")
    }
}

tasks {
    val runCommandInsideDocker by registering(CommandInvocation::class) {
        commandList.set(listOf("uname", "-a"))
    }
}

