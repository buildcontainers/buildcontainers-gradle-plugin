plugins {
    base
    `kotlin-dsl`
    groovy
    `maven-publish`
    `java-gradle-plugin`
    id("pl.allegro.tech.build.axion-release") version "1.10.0"
    id("com.gradle.plugin-publish") version "0.10.1"
}

repositories {
    mavenCentral()
}

group = "com.github.buildcontainers"

dependencies {
    implementation("javax.activation:activation:1.1.1")
    implementation("com.github.docker-java:docker-java:3.1.0-rc-7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.3.10")
    testImplementation("commons-io:commons-io:2.6")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5") {
        exclude(group = "org.codehaus.groovy")
    }
}

pluginBundle {
    website = "https://github.com/buildcontainers/buildcontainers-gradle-plugin"
    vcsUrl = "https://github.com/buildcontainers/buildcontainers-gradle-plugin.git"
    tags = listOf("build", "container", "docker", "vm", "ci", "continuous", "integration")
}

gradlePlugin {
    plugins {
        create("buildcontainers-plugin") {
            id = "com.github.buildcontainers"
            displayName = "buildcontainers gradle plugin"
            description = "A plugin that allows you to build and test your project inside container."
            implementationClass = "com.github.buildcontainers.BuildContainersPlugin"
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.wrapper {
    gradleVersion = "5.2.1"
    distributionType = Wrapper.DistributionType.ALL
}

project.version = scmVersion.version