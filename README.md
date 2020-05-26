![Java CI with Maven](https://github.com/lexemmens/podman-maven-plugin/workflows/Java%20CI%20with%20Maven/badge.svg) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=lexemmens_podman-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=lexemmens_podman-maven-plugin) [![Maven Central](https://img.shields.io/maven-central/v/nl.lexemmens/podman-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22nl.lexemmens%22%20AND%20a:%22podman-maven-plugin%22)

# Podman Maven Plugin
A maven plugin to build, tag and push OCI compliant images configured using a Dockerfile and built with Podman.

## About
This plugin was created based on the need to build container images with Podman, whilst not having an appropriate plugin to do so. Initially
using the [docker-maven-plugin](https://github.com/fabric8io/docker-maven-plugin) from `fabric8io` it quickly became clear that simply swapping
Docker for Podman (`alias docker=podman`) did not work as the plugin requires a Docker outlet to communicate with. With other plugins currently 
not yet available, the idea was born.

This plugin uses a `ProcessExecutor` from ZeroTurnaround to do its magic under water. Although this plugins' structure is similar to that from the 
`docker-maven-plugin`, its implementation is not. In an effort to keep things simple, I have chosen to use a `ProcessExecutor` to run `podman` rather
than implementing the logic myself. 

Also, only a subset of the `podman`s capabilities have been implemented (build, push and save).

## Requirements
- Maven 3.6.2 or later
- Java 9 or later
- Podman to be installed on the OS where this plugin will run
- Registry configuration to be present in Maven settings.

## Goals
| Goal                                             | Description                | Default Lifecycle Phase |
| -------------------------------------------------| -------------------------- | ----------------------- |
| `podman:build`                                   | Build images               | install                 | 
| `podman:push`                                    | Push images to a registry  | deploy                  |
| `podman:save`                                    | Save image to a file       |                         |

## Usage
The plugin is currently **NOT** available via Maven Central and can be used as follows:

1. Ensure that there is a `Dockerfile` places in the module's root directory
2. Configure the plugin in your pom file, like so: 
```XML
<plugin>
    <groupId>nl.lexemmens</groupId>
    <artifactId>podman-maven-plugin</artifactId>
    <version>0.1.0</version>
    <configuration>
        <registries>
            <registry>registry.example.com</registry>
        </registries>
        <pushRegistry>registry.example.com</pushRegistry>
        <build>
            <name>your/image/name</name>
            <tags>
                <tag>${project.artifactId}</tag>
            </tags>
            
            <useMavenProjectVersion>true</useMavenProjectVersion>
            
            <labels>
                <label-x>label-x-value</label-x>
            </labels>
        </build>
    </configuration>
</plugin>
```

You can also call this plugin from the command line by invoking either of its Mojo's via `mvn podman:build` or `mvn podman:push` after running a `mvn install`.

### Configuration
With respect to configuration, it is important to outline the difference to the plugins configuration and configuration that is required by the plugin to work.

#### Mandatory configuration
This plugin requires for _all_ configured registries to have credentials present in the Maven settings file (usually located in `~/.m2/settings.xml`). Registry
configuration should look like this:
```XML
<server>
  <id>examle-registry.com</id>
  <username>username</username>
  <password>password</password>
</server>
```

The password may be encrypted. The id of the server **must** match the registries configured in the plugin (see below). The plugin will fail if
credentials are missing for any of the provided registries.

This plugin will also fail if there are no registries configured, but authentication is not skipped. Please refer to the table below for all configuration
options.

#### Configuration parameters 
The following command line parameters are supported by this plugin:

| Parameter                 | Command line alias             | Type    | Required | Required by                                  | Default value      | Description                                    |
| ------------------------- | ------------------------------ | ------- | -------- | -------------------------------------------- | ------------------ | ---------------------------------------------- |
| registries                | podman.registries              | Array   | Y        | `podman:build`, `podman:save`, `podman:push` | -                  | All registries this plugin might reach out to during execution (for building (i.e. pulling), pushing and saving) |
| pushRegistry              | podman.image.target.registry   | String  | Y        | `podman:build`, `podman:push`                | -                  | The target registry where the container image will be pushed to |
| tlsVerify                 | podman.tls.verify              | Enum    | N        | `podman:build`, `podman:save`, `podman:push` | NOT_SPECIFIED      | Allows setting of the --tls-verify command when building, pushing or saving container images. When not specified this will fallback to default `Podman` behavior |
| skip                      | podman.skip                    | Boolean | N        | `podman:build`, `podman:save`, `podman:push` | false              | Skip all actions. |
| skipBuild                 | podman.skip.build              | Boolean | N        | `podman:build`                               | false              | Skip building container image |
| skipTag                   | podman.skip.tag                | Boolean | N        | `podman:build`                               | false              | Skip tagging container image after build |
| skipPush                  | podman.skip.push               | Boolean | N        | `podman:push`                                | false              | Will skip pushing the container image to the `targetRegistry` |
| deleteLocalImageAfterPush | podman.image.delete.after.push | Boolean | N        | `podman:push`                                | false              | Will delete the final image from the local registry. **NOTE:** All other pulled images (such as base images) will continue to exist. |
| skipSave                  | podman.skip.save               | Boolean | N        | `podman:save`                                | false              | Will skip saving the container image |
| skipAuth                  | podman.skip.auth               | Boolean | N        | `podman:build`, `podman:save`, `podman:push` | false              | Skip registry authentication check at the beginning. **NOTE:** This may cause access denied errors when building, pushing or saving container images. |

#### Configuration options in the pom file
dfdfdfdf 

### Using parameters in your Dockerfile
It is possible to specify properties in your pom file and use those properties in the Dockerfile, just like you would in a pom file:
```Dockerfile
FROM ${container.base.image}

WORKDIR /application
COPY ${project.artifactId}.jar ./
```

## Contributing
Feel free to open a Pull Request if you want to contribute!
