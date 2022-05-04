package nl.lexemmens.podman.config.skopeo.copy;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public class SkopeoCopySourceConfiguration {

    @Parameter
    protected File sharedBlobDir;

    public File getSharedBlobDir() {
        return sharedBlobDir;
    }
}
