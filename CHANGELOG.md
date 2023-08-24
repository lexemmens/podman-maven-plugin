## Changelog
### 1.18.0 (24-08-2023)
#### Bugs
* ([#89](https://github.com/lexemmens/podman-maven-plugin/issues/89)) - plugin fails with NullPointerException. Thanks to [kumadee](https://github.com/kumadee).

#### Improvements
* ([#88](https://github.com/lexemmens/podman-maven-plugin/issues/88)) - Unable to build a target stage in Multi-stage container buil. Thanks to [kumadee](https://github.com/kumadee).

#### Miscelaneous
* ([#87](https://github.com/lexemmens/podman-maven-plugin/issues/87)) - Documentation is not in line with implementation. Thanks to [kumadee](https://github.com/kumadee).

### 1.17.0 (09-07-2023)
#### Bugs
* ([#84](https://github.com/lexemmens/podman-maven-plugin/pull/84)) - Fix plugin functionality on Windows. Thanks to [Laurent Granié](https://github.com/lgranie).

#### Improvements
* ([#86](https://github.com/lexemmens/podman-maven-plugin/pull/86)) - Introduced support for Podman's `--platform` argument. Thanks to [rajha-korithrien](https://github.com/rajha-korithrien).

### 1.16.0 (06-06-2023)
Due to an administrative error, something went wrong in the versioning of the previous releases. As of 1.16.0 this is now corrected

#### Bugs
* ([#83](https://github.com/lexemmens/podman-maven-plugin/pull/83)) -  Skip copy goal if no container catalog artifact is found
* ([#81](https://github.com/lexemmens/podman-maven-plugin/pull/81)) -  Re-added missing logging of podman build steps

### 1.15.1 (18-04-2023)
#### Bugs
* ([#80](https://github.com/lexemmens/podman-maven-plugin/issues/80)) - SecurityContextService now derives graphRoot via `podman system info` 

### 1.14.0 (11-04-2023)
#### Bugs
* ([#75](https://github.com/lexemmens/podman-maven-plugin/issues/75)) - Podman Clean was not working after version 1.11.0
* ([#76](https://github.com/lexemmens/podman-maven-plugin/issues/76)) - Podman Push goal failed when no container-catalog.txt is found
* ([#77](https://github.com/lexemmens/podman-maven-plugin/issues/77)) - SELinux context was not set correctly when using custom root

### 1.13.0 (05-04-2023)
#### Improvements
* ([#59](https://github.com/lexemmens/podman-maven-plugin/issues/59)) - Support for build arguments

#### Bugs
* ([#74](https://github.com/lexemmens/podman-maven-plugin/issues/74)) - Fixed cataloguing of artifacts when catloguing is skipped of no valid images are present

### 1.12.0 (21-02-2023)
#### Improvements
* ([#69](https://github.com/lexemmens/podman-maven-plugin/issues/69)) - Add SELinux support when building container images (RHEL8/CentOS8)
* ([#71](https://github.com/lexemmens/podman-maven-plugin/pull/71)) - Add possibility to set cgroup manager (`--cgroup-manager` equivalent)

### 1.11.1 (23-08-2022)
#### Bugs
* ([#67](https://github.com/lexemmens/podman-maven-plugin/pull/67)) - Execution of Mojo's was not properly skipped if corresponding goal is skipped.

### 1.11.0 (28-07-2022)
#### Bugs
* ([#65](https://github.com/lexemmens/podman-maven-plugin/pull/65)) - Update PushMojo to use container-catalog.txt. Also prevent accidental duplicate push of container iamges

### 1.10.0 (19-07-2022)
#### Improvements
* Added copy mojo that allows copying images from one repo to another (uses Skopeo)

### 1.9.0 (28-06-2022)
#### Bugs
* ([#57](https://github.com/lexemmens/podman-maven-plugin/issues/57)) - mockito-junit-jupiter test dependency not scoped test
* ([#60](https://github.com/lexemmens/podman-maven-plugin/pull/60) - Fix `%a` placeholder for image names
* ([#62](https://github.com/lexemmens/podman-maven-plugin/pull/50) - Fix AuthenticationServiceTest to use dynamic uid

#### Improvements
* Added configuration options for `--squash`, `--squash-all` and `--layers`. Can be configured in the `<build>` section of an image.

### 1.8.0 (12-10-2021)
#### Bugs
* ([#49](https://github.com/lexemmens/podman-maven-plugin/issues/49)) - Using properties in the FROM now succeeds for multistage builds.
* ([#51](https://github.com/lexemmens/podman-maven-plugin/issues/51)) - Building projects with packaging=pom now succeed because e.g. the target directory will succesfully be created.

### 1.7.1 (16-08-2021)
#### Bugs
* ([#47](https://github.com/lexemmens/podman-maven-plugin/issues/47)) - Label values are now always put between double quotes.

### 1.7.0 (25-07-2021)
#### Bugs
* ([#42](https://github.com/lexemmens/podman-maven-plugin/issues/42)) - Podman `pull` and `pullAlways` options cannot be enabled simultaneously.
* ([#44](https://github.com/lexemmens/podman-maven-plugin/issues/44)) - Images are now by default saved to the target directory of the Maven module where Podman is configued.
* ([#41](https://github.com/lexemmens/podman-maven-plugin/issues/41)) - Implemented a warning marker when there is no image name configured for a particular stage.

#### Improvements
* ([#44](https://github.com/lexemmens/podman-maven-plugin/issues/44)) - SaveMojo: The location where images are saved on disk can now be configured. Also, the push registry is no longer part of the file name.

### 1.6.0 (25-03-2021)
#### Bugs
* ([#32](https://github.com/lexemmens/podman-maven-plugin/issues/32) - When tagging an image, the plugin might skip one or more steps due to unexpected multiline output for a build step)

#### Improvements
* ([#35](https://github.com/lexemmens/podman-maven-plugin/issues/35)) Introduced option to configure a batch of images to build, tag and push
* ([#38](https://github.com/lexemmens/podman-maven-plugin/issues/38) The plugin now checks `~/.docker/config.json` for credentials.
* ([#36](https://github.com/lexemmens/podman-maven-plugin/issues/36)) Implemented a retry mechanism for Podman push

#### Other
* This plugin now requires Java 8 rather than Java 9.
* Documentation has now moved towards [Github Pages](https://lexemmens.github.io/podman-maven-plugin/docs/1.6.0/).

### 1.5.0 (09-02-2021)
#### Bugs
* ([#28](https://github.com/lexemmens/podman-maven-plugin/issues/28)) Changed algorithm for image hash detection when using multistage containerfiles to use lookahead instead of look back.

#### Improvements
* ([#30](https://github.com/lexemmens/podman-maven-plugin/issues/30)) Introduced `pullAlways` option in build configuration whether an image should always be pulled. [More information](http://docs.podman.io/en/latest/markdown/podman-build.1.html#pull-always).

### 1.4.0 (25-11-2020)
#### Improvements
* ([#26](https://github.com/lexemmens/podman-maven-plugin/pull/26)) Introduced `pull` option in build configuration to control whether an image should be pulled. [More information](https://www.redhat.com/sysadmin/podman-image-pulling). Thanks to [Christopher J. Uwe](https://github.com/cruwe).

### 1.3.0 (18-11-2020)
#### Improvements
* When debug is enabled, Podman version information will now be printed.

#### Bugs
* Passwords that contained special characters were not always properly obfuscated

### 1.2.2 (28-10-2020)
#### Bugs
* Set the default run directory to the root of the current module instead of the root of the Maven project.

### 1.2.1 (28-10-2020)
#### Bugs
* Allow configuration of the directory where the Podman command is executed. This defaulted to the target directory, which is not always convenient.

### 1.2.0 (28-10-2020)
#### Improvements
* Introduced a flag called `failOnMissingContainerFile` that prevents that the plugin fail throw an exception in case no Containerfile is present in a Maven module. This may be useful
for projects that use this plugin as part of a profile where a `Containerfile` is not always in the same location

### 1.1.0 (16-10-2020)
#### Bugs
* Fixed an issue that required the `CleanMojo` to have a valid ImageConfiguration. This is not necessary since only the custom root storage location needs to be configured for this Mojo to work.

### 1.0.0 (20-10-2020)
#### Improvements
* Renamed `Docker` to `Container` to be in line with Podman's naming conventions. This is a breaking change, please update your configuration.
* Added basic support for multistage `Containerfile`s. Please refer to documentation for the exact configuration.
 
### 0.9.0 (22-09-2020)
#### Improvements
* Added configuration of the `--runroot` setting, which is used by Podman to store its state information.

### 0.8.0 (xx-09-2020)
#### Improvements
* Added clean phase that allows cleaning up the local storage. This only works when the root storage location is configured in the pom file to prevent accidentally deleting unrelated files.

### 0.7.0 (26-06-2020)
#### Bugs
* Fixed a NullPointerException that occurred when no image configuration was present in the pom. A normal exception with more information is now raised instead.

#### Improvements
* Added possibility to build container images with docker manifest and configuration data (`podman build --format=<oci/docker>` equivalent).

### 0.6.1 (05-06-2020)
#### Bugs
* Fixed a `NullPointerException` that could occur when no `<podman/>` configuration was specified. 

### 0.6.0 (04-06-2020)
#### Improvements
* Added option to set a custom root directory when running Podman (equivalent of running `podman --root=/some/custom/directory`)

### 0.5.0 (29-05-2020)
#### Bugs
* Labels were not always added on a new line
* Labels after an `ENTRYPOINT` command were sometimes ignored. Labels are now put on the line after the base image declaration (`FROM` command)

#### Improvements
* Documentation no longer states that this plugin is **not** in Maven Central. 

### 0.4.0 (28-05-2020)
#### Bugs
* Podman used the wrong base dir for building container images
* When TLS Verify is not set, this sometimes caused an '... takes only 1 argument' error, due to an empty subcommand being passed.

#### Improvements
* Changed the log line that says 'Initializing image configurations.' to debug.
* Explicitly set `dockerFileDir` to value of `${project.baseDir}` rather then `new File(".")`

### 0.3.0 (27-05-2020)
#### Bugs
* AuthenticationService now checks the default credential file based on the XDG_RUNTIME_DIR environment variable
* When TLS Verify is not set, it will no longer be used when running Podman commands.
* When Podman login failed, the password of the user was printed in the error message

#### Improvements
* Moved authentication to the different Mojo's, such that `skipBuild`, `skipPush`, `skipSave` can be configured separately. 

### 0.2.1 (26-05-2020)
#### Security
* Removed accidental publication of passphrase. Tag 0.2.0 has therefore been removed from Github.

### 0.2.0 (26-05-2020)
#### Improvements
* Refactored the iamge build configuration for more flexibility

#### New features
* Added noCache support when buildig images
* Added support for labels. Labels are metadata and will be appended to the source Dockerfile before running a build

_NOTE:_ This release is not compatible with 0.1.0. Due to the early stages of development you must reconfigure your plugin in the `pom.xml`

### 0.1.0 (24-05-2020)
Initial implementation released
