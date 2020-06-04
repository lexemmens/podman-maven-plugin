![Java CI with Maven](https://github.com/lexemmens/podman-maven-plugin/workflows/Java%20CI%20with%20Maven/badge.svg) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lexemmens_podman-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=lexemmens_podman-maven-plugin) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=lexemmens_podman-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=lexemmens_podman-maven-plugin) [![Maven Central](https://img.shields.io/maven-central/v/nl.lexemmens/podman-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22nl.lexemmens%22%20AND%20a:%22podman-maven-plugin%22)

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

This plugin is mainly inspired by the `docker-maven-plugin` and shows some similarities in configuration.

## Requirements
- Maven 3.6.2 or later
- Java 9 or later
- Podman to be installed on the OS where this plugin will run
- Registry configuration to be present in Maven settings.
- Dockerfile to be present in the module

NOTE: This Plugin only works when a Dockerfile is in your module's folder. You cannot create a complete Dockerfile via configuration in the `pom.xml`.

## Goals
| Goal                                             | Description                | Default Lifecycle Phase |
| -------------------------------------------------| -------------------------- | ----------------------- |
| `podman:build`                                   | Build images               | install                 | 
| `podman:push`                                    | Push images to a registry  | deploy                  |
| `podman:save`                                    | Save image to a file       |                         |

## Usage
The plugin is available via Maven Central and can be used as follows:

1. Ensure that there is a `Dockerfile` places in the module's root directory
2. Configure the plugin in your pom file, like so: 
```XML
<plugin>
    <groupId>nl.lexemmens</groupId>
    <artifactId>podman-maven-plugin</artifactId>
    <version>0.2.0</version>
    <configuration>
        <registries>
            <registry>registry.example.com</registry>
        </registries>
        <pushRegistry>registry.example.com</pushRegistry>
        <podman>
            <tlsVerify>true</tlsVerify>   
            <root>/path/to/a/folder</root>   
        </podman>
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
The following command line parameters are supported by this plugin. It is also possible to specify these parameters in the `<configuration>` section of the plugin in the `pom.xml`.

| Parameter                 | Command line alias             | Type    | Required | Required by                                  | Default value      | Description                                    |
| ------------------------- | ------------------------------ | ------- | -------- | -------------------------------------------- | ------------------ | ---------------------------------------------- |
| registries                | podman.registries              | Array   | Y        | `podman:build`, `podman:save`, `podman:push` | -                  | All registries this plugin might reach out to during execution (for building (i.e. pulling), pushing and saving) |
| pushRegistry              | podman.image.push.registry     | String  | Y        | `podman:build`, `podman:push`                | -                  | The target registry where the container image will be pushed to |
| skip                      | podman.skip                    | Boolean | N        | `podman:build`, `podman:save`, `podman:push` | false              | Skip all actions. |
| skipBuild                 | podman.skip.build              | Boolean | N        | `podman:build`                               | false              | Skip building container image |
| skipTag                   | podman.skip.tag                | Boolean | N        | `podman:build`                               | false              | Skip tagging container image after build |
| skipPush                  | podman.skip.push               | Boolean | N        | `podman:push`                                | false              | Will skip pushing the container image to the `targetRegistry` |
| deleteLocalImageAfterPush | podman.image.delete.after.push | Boolean | N        | `podman:push`                                | false              | Will delete the final image from the local registry. **NOTE:** All other pulled images (such as base images) will continue to exist. |
| skipSave                  | podman.skip.save               | Boolean | N        | `podman:save`                                | false              | Will skip saving the container image |
| skipAuth                  | podman.skip.auth               | Boolean | N        | `podman:build`, `podman:save`, `podman:push` | false              | Skip registry authentication check at the beginning. **NOTE:** This may cause access denied errors when building, pushing or saving container images. |

#### Configuration options in the pom file
In the `<configuration />` section of your pom, there are some other options you may apply when building container images using this plugin and `Podman`.
The configuration parameters listed in the table above are also supported here.

The example in XML below lists all the other configuration options that are possible:

```xml
<plugin>
    <groupId>nl.lexemmens</groupId>
    <artifactId>podman-maven-plugin</artifactId>
    <configuration>
        <!-- See previous section for properties at this level -->
        <podman>
            <tlsVerify>NOT_SPEFICIED</tlsVerify>    
            <root>/path/to/a/folder</root>    
        </podman>

        <images>
            <image>
                <name>my/podman/image</name>
                <build>
                    <noCache>true</noCache>
                    <dockerFile>Dockerfile</dockerFile>
                    <dockerFileDir>path/to/a/directory</dockerFileDir>

                    <tagWithMavenProjectVersion>false</tagWithMavenProjectVersion>
                    <createLatestTag>false</createLatestTag>

                    <tags>
                        <tag>1.0.0-SNAPSHOT</tag>
                        <tag>0.3.0-beta</tag>
                    </tags>

                    <labels>
                        <label-one>value-one</label-one>
                    </labels>
                </build>
            </image>
        </images>

        <tlsVerify>FALSE</tlsVerify>
    </configuration>
</plugin>
```

##### Podman Configuration Options
The tables below explains the global configuration options for podman that were used in the example above:

| Parameter                | Type    | Required | Default value      | Description |
|--------------------------|---------|----------|--------------------|-------------|
| tlsVerify                | Boolean | N        | -                  | Allows explicit control of TLS Verification |
| root                     | String  | N        | -                  | Controls the storage location that Podman should use when building containers |

##### Image Configuration Options
The tables below explains the configuration options for container images that were used in the example above:

| Parameter                | Type    | Required | Default value      | Description |
|--------------------------|---------|----------|--------------------|-------------|
| name                     | String  | Y        | -                  | The name of the docker image without the registry or tag. May contain a repository. Example: `some/repo/image`. [Docker naming conventions](https://docs.docker.com/engine/reference/commandline/tag/) apply. |
| noCache                  | Boolean | N        | false              | Do not use cache when building the image |
| dockerFile               | String  | N        | Dockerfile         | Allows using a Dockerfile that is named differently |
| dockerFileDir            | String  | N        | Current Module Dir | Specifies in which directory to find the Dockerfile |
| tagAsMavenProjectVersion | Boolean | N        | false              | When set to true, the container image will automatically be tagged with the version of the Maven project. Note that a container can receive one or more tags whilst maintaining the same name. |
| createLatestTag          | Boolean | N        | false              | When set to true, the container image will automatically be tagged 'latest'. Note that a container can receive one or more tags whilst maintaining the same name. |
| labels                   | Map     | N        | -                  | When set, the Dockerfile will be decorated with the specified labels. Useful when adding metadata to your images |

### Using parameters in your Dockerfile
It is possible to specify properties in your pom file and use those properties in the Dockerfile, just like you would in a pom file:
```Dockerfile
FROM ${container.base.image}

WORKDIR /application
COPY ${project.artifactId}.jar ./
```

## Troubleshooting
This plugin takes the Dockerfile at the configured location as input and copies it to another location (usually the target folder) to decorate it. Please
have a look at the Dockerfile in the target golder first before creating an issue. 

## Contributing
Feel free to open a Pull Request if you want to contribute!
