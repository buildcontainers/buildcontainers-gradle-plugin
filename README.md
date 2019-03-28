# Buildcontainers [![Build Status](https://travis-ci.org/buildcontainers/buildcontainers-gradle-plugin.svg?branch=master)](https://travis-ci.org/buildcontainers/buildcontainers-gradle-plugin)

Buildcontainers is gradle plugin that allows you to build and test your project inside docker container. Basically
all you need to do is to define build environment and specify which tasks you want to run. Main purpose is to make it
easier to define environments for CI.

## Benefits
- Project build environment is CI-independent

- Build environment can be stored in project's repository

- Project can be built anywhere with the same environment - whether it's developer's machine or CI server. This means
  easy troubleshooting

- Better isolation on multi-project CI servers like Jenkins - every project can have it's own environment

## Requirements
Plugin requires Gradle 5.2

## Basic setup
In order to start using plugin, you need to:

Apply plugin
```groovy
plugins {
    id("com.github.buildcontainers") version "0.1.0"
}
```

Define tasks that will be executed inside docker

```groovy
import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

task buildInsideDocker(type: GradleWrapperInvocation) {
    argumentList = ["build"]
}
```

That's all! Task `buildInsideDocker` will execute gradle task `build` inside container.

Check `examples` directory to play around with working examples.

## User guide

In this section we will inspect all available configuration properties exposed by buildcontainers plugin.

### Define build environment

With buildcontainers you can define your build environment in two ways

* with base image – default option

    Simply choose one of images available in https://index.docker.io/v1/. Default image is `openjdk:8-alpine`.
    
    ```groovy
    import com.github.buildcontainers.extension.EnvironmentDefinitionType
    
    buildcontainers {
        environmentDefinitionType = EnvironmentDefinitionType.BASE_IMAGE
        
        baseImageEnvironment {
            tag = 'openjdk:9-slim'
        }
    }
    ```

* with dockerfile

    Prepare Dockerfile which describes your build environment.

    ```groovy
    import com.github.buildcontainers.extension.EnvironmentDefinitionType
    
    buildcontainers {
        environmentDefinitionType = EnvironmentDefinitionType.DOCKERFILE
        
        dockerfileEnvironment {
            dockerfile = file('docker/Dockerfile')
            baseDirectory = file('docker')
        }
    }
    ```
    
    `dockerfile` is a obligatory property which points to your Dockerfile. `baseDirectory` is a base directory used 
    to build environment image. By default this property points to parent directory of `dockerfile`.

### Configure your build environment

Once you have defined your build environment you may want to configure how build is executed.

* user permission

    Buildcontainers mount your project directory inside docker container. It means that build command will affect 
    your host file system directly. For this reason it is crucial to properly define user-inside-container permissions.
    Buildcontainers allows you to fully control those permissions. You can set `UID`, `GUID` and all groups that 
    user-inside-container should be assigned to.
    
    ```groovy   
    buildcontainers {
        buildEnvironment {
            userId = "1000"
            groupId = "1000"
            groups = ["1001", "1002"]
        }
    }
    ```
    
    Default values of `userId`, `groupId` and `groups` are respectively outputs of `id -u`, `id -g` and `id -G`. As 
    those defaults imitate your host-user permissions it is very likely that you won't need to change them.
    
* gradle cache

    When you run gradle task inside container you need to download wrapper from scratch and fill your gradle 
    dependencies cache. So it is very likely that you would like to persist contents of gradle user home and gradle 
    project cache directories between builds. This plugin let's you to mount them on your host file system.
    
    ```groovy
    buildcontainers {
        buildEnvironment {
            gradleUserHome = file(".gradle-docker/user-home")
            gradleProjectCacheDir = file(".gradle-docker/project-cache")
        }
    }
    ```
    
    `gradleUserHome` and `gradleProjectCacheDir` default values are respectively 
    `~/.buildcontainers/<PROJECT-NAME>/gradle/user-home` and 
    `~/.buildcontainers/<PROJECT-NAME>/gradle/project-cache-dir`.

* docker socket path

    When your build process produce docker image or when your integration tests involve use of docker container then
    it's handy to access docker. You can mount docker socket with the following configuration.
    
    ```groovy
    buildcontainers {
        buildEnvironment {
            dockerSocketPath = "/var/run/docker.sock"
        }
    }
    ```
    
    When `dockerSocketPath` is not defined then docker sock won't be mounted.
    
* build logs

    By default buildcontainers will forward whole output from inside container to your console. You can customize this
    output by adding timestamps.
    
    ```groovy
    buildcontainers {
        buildEnvironment {
            displayTimestamps = true
        }
    }
    ```
    
    Default value of `displayTimestamps` property is `false`.
    
### Configure docker

Docker host can be configured in the following way

```groovy
buildcontainers {
    docker {
        dockerHost = "unix:///var/run/docker.sock"
    }
}
```

Default value of `dockerHost` is `"unix:///var/run/docker.sock"`.

### Define your tasks

Buildcontainers allows you to run two kind of tasks inside container

#### Gradle wrapper invocation

Invokes gradle wrapper inside container. It is the same as running `./gradlew <argumentList>` in your project.

```groovy
import com.github.buildcontainers.task.invocation.GradleWrapperInvocation

task invoke(type: GradleWrapperInvocation) {
    argumentList = [ "tasks", "--all" ]
}
```

`argumentList` holds list of arguments passed to `./gradlew` inside container.

#### Command invocation

Invokes any command inside container.

```groovy
import com.github.buildcontainers.task.invocation.CommandInvocation

task invoke(type: CommandInvocation) {
     commandList = [ "cat", "/etc/hosts" ]
 }
```

## License

**buildcontainers** gradle plugin is published under MIT license. 

## Authors
[Przemysław Piórkowski](https://github.com/piorkowskiprzemyslaw), [Maciej Sabat](https://github.com/msabat)
