package nl.lexemmens.podman.service;

import nl.lexemmens.podman.command.buildah.BuildahUnshareCommand;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

/**
 * Class that allows very specific execution of Podman related commands.
 */
public class BuildahExecutorService {

    private final Log log;
    private final CommandExecutorDelegate delegate;
    private final File podmanRoot;


    /**
     * Constructs a new instance of this class.
     *
     * @param log          Used to access Maven's log system
     * @param podmanConfig Contains Podman specific configuration, such as tlsVerify and podman's root directory
     * @param delegate     A delegate executor that executed the actual command
     */
    public BuildahExecutorService(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        this.log = log;
        this.delegate = delegate;
        this.podmanRoot = podmanConfig.getRoot();
    }

    /**
     * <p>
     * Implementation of the 'buildah unshare' command
     * </p>
     * <p>
     * Attempts to clean up the local storage location by running the buildah unshare command. This only works
     * when the podman root is configured in the pom file.
     * </p>
     * <p>
     * To avoid accidentally clean up other files, this method does nothing when podman root is not specified.
     * </p>
     *
     * @throws MojoExecutionException In case execution of the buildah unshare command fails
     */
    public void cleanupLocalContainerStorage() throws MojoExecutionException {
        if (podmanRoot == null) {
            log.info("Podman root storage location is set to its defaults. Not cleaning up this storage location.");
            return;
        }

        new BuildahUnshareCommand.Builder(log, delegate)
                .removeDirectory(podmanRoot.getAbsolutePath())
                .build()
                .execute();
    }


}
