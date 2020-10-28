package nl.lexemmens.podman.image;

import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;

public class TestPodmanConfigurationBuilder {

    private final PodmanConfiguration podman;

    public TestPodmanConfigurationBuilder() {
        podman = new PodmanConfiguration();
    }

    public TestPodmanConfigurationBuilder setTlsVerify(TlsVerify tlsVerify) {
        podman.tlsVerify = tlsVerify;
        return this;
    }

    public TestPodmanConfigurationBuilder setRoot(File root) {
        podman.root = root;
        return this;
    }

    public TestPodmanConfigurationBuilder setRunRoot(File runRoot) {
        podman.runRoot = runRoot;
        return this;
    }

    public TestPodmanConfigurationBuilder setRunDirectory(File runDirectory) {
        podman.runDirectory = runDirectory;
        return this;
    }

    public TestPodmanConfigurationBuilder initAndValidate(MavenProject project, Log log) throws MojoExecutionException {
        podman.initAndValidate(project, log);
        return this;
    }

    public PodmanConfiguration build() {
        return podman;
    }

}
