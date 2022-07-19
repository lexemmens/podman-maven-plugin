![Java CI with Maven](https://github.com/lexemmens/podman-maven-plugin/workflows/Java%20CI%20with%20Maven/badge.svg) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lexemmens_podman-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=lexemmens_podman-maven-plugin) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=lexemmens_podman-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=lexemmens_podman-maven-plugin) [![Maven Central](https://img.shields.io/maven-central/v/nl.lexemmens/podman-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22nl.lexemmens%22%20AND%20a:%22podman-maven-plugin%22)

# Podman Maven Plugin
A maven plugin to build, tag and push OCI compliant images configured using a Dockerfile/Containerfile using Podman.

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

## Goals
| Goal           | Description                                                                                             | Default Lifecycle Phase |
|----------------|---------------------------------------------------------------------------------------------------------| ----------------------- |
| `podman:build` | Build images                                                                                            | install                 | 
| `podman:push`  | Push images to a registry                                                                               | deploy                  |
| `podman:copy`  | Copy images from the registry used during push to a configured registry (uses skopeo instead of podman) | deploy                  |
| `podman:clean` | Clean up local storage                                                                                  | clean                   |
| `podman:save`  | Save image to a file                                                                                    |                         |

## Documentation
The manual of this plugin is available on [GitHub Pages](https://lexemmens.github.io/podman-maven-plugin/docs/index.html)

## Contributing
Feel free to open a Pull Request if you want to contribute!
