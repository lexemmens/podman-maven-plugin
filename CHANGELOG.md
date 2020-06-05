## Changelog

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