# Podman Maven Plugin
A maven plugin to build, tag and push OCI compliant images built with Podman.

## About
As Started as a plugin to play around with building Maven plugins and processes, this project contains a Maven plugin
that can be used to build, tag and push container images using Podman.

The maven plugin presented in this project is in no way perfect and some things probably should be done differently. However, in the absence of a plugin that
is capable of building container images using Podman, I felt that this plugin at least fills in a void.

## Contributing
Please feel free to open a pull request if you think you can help make things better.

## Usage
The plugin is available via Maven Central and can be used as follows:

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
                <goal>push</goal>
            </goals>
            <configuration>
                <tags>
                    <tag>${project.artifactId}:${project.version}</tag>
                </tags>
            </configuration>
        </execution>
    </executions>
</plugin>
```