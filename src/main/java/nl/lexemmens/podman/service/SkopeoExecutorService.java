package nl.lexemmens.podman.service;

import nl.lexemmens.podman.command.skopeo.SkopeoCopyCommand;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Enables executing the skopeo binary with specific arguments.
 */
public class SkopeoExecutorService {

    private final Log log;
    private final CommandExecutorDelegate delegate;
    private final SkopeoConfiguration skopeoConfiguration;

    /**
     * Constructs a new instance of this class.
     *
     * @param log                 Used to access Maven's log system
     * @param skopeoConfiguration Contains skopeo specific configuration, such as getSrcTlsVerify
     * @param delegate            A delegate executor that executed the actual command
     */
    public SkopeoExecutorService(Log log, SkopeoConfiguration skopeoConfiguration, CommandExecutorDelegate delegate) {
        this.log = log;
        this.skopeoConfiguration = skopeoConfiguration;
        this.delegate = delegate;
    }

    /**
     * Implementation of the skopeo copy command.
     *
     * @param sourceImage      source image to copy
     * @param destinationImage target for the image
     * @throws MojoExecutionException In case the skopeo copy command exits unsuccessfully.
     */
    public void copy(String sourceImage, String destinationImage) throws MojoExecutionException {
        new SkopeoCopyCommand.Builder(log, skopeoConfiguration, delegate)
                .setSourceImage(sourceImage)
                .setDestinationImage(destinationImage)
                .build()
                .execute();
    }
}
