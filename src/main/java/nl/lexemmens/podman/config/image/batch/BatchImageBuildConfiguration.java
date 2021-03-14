package nl.lexemmens.podman.config.image.batch;

import nl.lexemmens.podman.config.image.AbstractImageBuildConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

public class BatchImageBuildConfiguration extends AbstractImageBuildConfiguration {

    /**
     * Root directory in which the Containerfiles should be searched. Search is recursive.
     */
    @Parameter
    protected File containerFileRootDir;

    /**
     * Constructor
     */
    public BatchImageBuildConfiguration() {
        // Empty - will be injected
    }

    /**
     * Validates this class by giving all null properties a default value.
     *
     * @param project The MavenProject used to derive some of the default values from.
     * @param log     Access to Maven's log system for writing errors
     * @param failOnMissingContainerfile Whether an exception should be thrown if no Containerfile is found
     * @throws MojoExecutionException In case there is no Containerfile at the specified source location or the Containerfile is empty
     */
    @Override
    public void validate(MavenProject project, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        super.validate(project, log, failOnMissingContainerfile);

        // TODO Find all Containerfiles and ensure they are not empty
    }
}
