package nl.lexemmens.podman.config.skopeo.copy;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public class SkopeoCopyDestinationConfiguration {

    @Parameter
    protected File sharedBlobDir;

    @Parameter
    protected Boolean precomputeDigests;

    @Parameter
    protected Boolean ociAcceptUncompressedLayers;

    @Parameter
    protected Boolean decompress;

    @Parameter
    protected String dockerDaemonHost;

    @Parameter
    protected String compressLevel;

    @Parameter
    protected String compressFormat;

    @Parameter
    protected Boolean compress;

    @Parameter
    protected File certDir;


    public File getSharedBlobDir() {
        return sharedBlobDir;
    }

    public Boolean getPrecomputeDigests() {
        return precomputeDigests;
    }

    public Boolean getOciAcceptUncompressedLayers() {
        return ociAcceptUncompressedLayers;
    }

    public Boolean getDecompress() {
        return decompress;
    }

    public String getDockerDaemonHost() {
        return dockerDaemonHost;
    }

    public String getCompressLevel() {
        return compressLevel;
    }

    public String getCompressFormat() {
        return compressFormat;
    }

    public Boolean getCompress() {
        return compress;
    }

    public File getCertDir() {
        return certDir;
    }
}
