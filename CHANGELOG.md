## Changelog

### 0.3.0 (27-05-2020)
#### Bugs
* AuthenticationService now checks the default credential file based on the XDG_RUNTIME_DIR environment variable
* When TLS Verify is not set, it will no longer be used when running Podman.

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