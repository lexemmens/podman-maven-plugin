package nl.lexemmens.podman.image;

import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

import static nl.lexemmens.podman.enumeration.TlsVerify.NOT_SPECIFIED;

/**
 * Holds all configuration that should be applied when executing the Podman command, such as whether TLS Verification should be used
 * and which directory Podman should use as its base/root directory.
 */
public class PodmanConfiguration {

    /**
     * Whether Podman should verify TLS/SSL certificates. Defaults to true.
     */
    @Parameter(property = "podman.tls.verify", defaultValue = "NOT_SPECIFIED", required = true)
    protected TlsVerify tlsVerify;

    /**
     * Podman's root directory.
     * <p>
     * Storage root dir in which data, including images, is stored (default: “/var/lib/containers/storage” for UID 0,
     * “$HOME/.local/share/containers/storage” for other users). Default root dir configured in /etc/containers/storage.conf.
     */
    @Parameter(property = "podman.root")
    protected File root;

    /**
     * Podman's root directory for state information.
     * <p>
     * Storage state directory where all state information is stored (default: “/var/run/containers/storage” for UID 0,
     * “/var/run/user/$UID/run” for other users). Default state dir configured in /etc/containers/storage.conf.
     */
    @Parameter(property = "podman.run.root")
    protected File runRoot;

    /**
     * Constructor
     */
    public PodmanConfiguration() {
        // Empty - will be injected
    }

    /**
     * Returns the value of the tlsVerify setting
     *
     * @return The value of the TLS Verify setting
     */
    public TlsVerify getTlsVerify() {
        return tlsVerify;
    }

    /**
     * Returns the root directory that Podman should use
     *
     * @return Podman's root directory
     */
    public File getRoot() {
        return root;
    }

    /**
     * Returns the runroot directory that Podman should use for saving state information
     *
     * @return Podman's runroot directory
     */
    public File getRunRoot() {
        return runRoot;
    }

    /**
     * Validates and initializes this configuration
     *
     * @param log Access to Maven's log system for informational purposes.
     * @throws MojoExecutionException In case validation fails.
     */
    public void initAndValidate(Log log) throws MojoExecutionException {
        if (tlsVerify == null) {
            log.debug("Setting TLS Verify to NOT_SPECIFIED");
            tlsVerify = NOT_SPECIFIED;
        } else {
            log.info("Setting tlsVerify to: " + tlsVerify.name());
        }

        if (root == null) {
            log.debug("Using Podman's default settings for --root.");
        } else {
            log.info("Setting Podman's 'root' directory to: " + root.getAbsolutePath());
        }

        if (runRoot == null) {
            log.debug("Using Podman's default settings for --runroot.");
        } else {
            log.info("Setting Podman's 'runroot' directory to: " + runRoot.getAbsolutePath());
        }
    }
}
