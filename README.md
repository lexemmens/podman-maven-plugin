![Java CI with Maven](https://github.com/lexemmens/podman-maven-plugin/workflows/Java%20CI%20with%20Maven/badge.svg) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lexemmens_podman-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=lexemmens_podman-maven-plugin)

# Podman Maven Plugin
A maven plugin to build, tag and push OCI compliant images configured using a Dockerfile and built with Podman.

## Disclaimer
This plugin is currently under development. Not all features may work as expected.

## About
Started as a plugin to play around with building Maven plugins and processes, this project contains a Maven plugin
that can be used to build, tag and push container images using Podman. It uses Podman by spawning a process under the hood which calls
the regular `podman build`, `podman tag` and `podman push` commands.

The maven plugin presented in this project is in no way perfect and some things probably should be done differently. However, in the absence of a plugin that
is capable of building container images using Podman, this plugin gets the job done and I felt that this plugin at least fills in a void.

## Contributing
Please feel free to open a pull request if you think you can help make things better.

## Usage
The plugin is currently **NOT** available via Maven Central and can be used as follows:

1. Ensure that there is a `Dockerfile` places in the module's root directory
2. Configure the plugin in your pom file, like so: 
```XML
<plugin>
    <groupId>nl.lexemmens</groupId>
    <artifactId>podman-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <phase>install</phase>
            <goals>
                <goal>build</goal>
            </goals>
            <configuration>
                <tags>
                    <tag>YOUR_TAG_HERE:YOUR_VERSION</tag>
                </tags>
            </configuration>
        </execution>
    </executions>
</plugin>
```

You can also call this plugin from the command line by invoking either of its Mojo's via `mvn podman:build` or `mvn podman:push`.

### Using parameters in your Dockerfile
It is possible to specify properties in your pom file and use those properties in the Dockerfile, just like you would in a pom file:
```Dockerfile
FROM ${container.base.image}

WORKDIR /application
COPY piradio-deployment-1.0.0-SNAPSHOT-all.tar ./
```