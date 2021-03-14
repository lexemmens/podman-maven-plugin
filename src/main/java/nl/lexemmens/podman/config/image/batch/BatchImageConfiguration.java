package nl.lexemmens.podman.config.image.batch;

import nl.lexemmens.podman.config.image.AbstractImageConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Holds the configuration for the container images that are being built. Values of this class will be set via
 * the Maven pom, except for the image hash.
 */
public class BatchImageConfiguration extends AbstractImageConfiguration {

    /**
     * The build image configuration.
     */
    @Parameter
    protected BatchImageBuildConfiguration build;

    /**
     * <p>
     * Returns the build configuration
     * </p>
     *
     * @return the configuration used for building the image
     */
    @Override
    public BatchImageBuildConfiguration getBuild() {
        return build;
    }

    /**
     * Initializes this configuration and fills any null values with default values.
     *
     * @param mavenProject               The MavenProject to derive some of the values from
     * @param log                        The log for logging any errors that occur during validation
     * @param failOnMissingContainerfile Whether an exception should be thrown if no Containerfile is found
     * @throws MojoExecutionException In case validation fails.
     */
    public void initAndValidate(MavenProject mavenProject, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        super.initAndValidate(mavenProject, log, failOnMissingContainerfile);

        build.validate(mavenProject, log, failOnMissingContainerfile);
    }
}
