package nl.lexemmens.podman.config.image.single;

import nl.lexemmens.podman.config.image.AbstractImageConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Holds the configuration for the container images that are being built. Values of this class will be set via
 * the Maven pom, except for the image hash.
 */
public class SingleImageConfiguration extends AbstractImageConfiguration<SingleImageBuildConfiguration> {

    /**
     * The build image configuration.
     */
    @Parameter
    protected SingleImageBuildConfiguration build;

    /**
     * Initializes this configuration and fills any null values with default values.
     *
     * @param mavenProject               The MavenProject to derive some of the values from
     * @param log                        The log for logging any errors that occur during validation
     * @param failOnMissingContainerfile Whether an exception should be thrown if no Containerfile is found
     * @throws MojoExecutionException In case validation fails.
     */
    public void initAndValidate(MavenProject mavenProject, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        super.initAndValidate(log);

        if (build == null) {
            log.error("Missing <build/> section in image configuration!");
            throw new MojoExecutionException("Missing <build/> section in image configuration!");
        }

        if (build.isMultistageContainerFile() && !customImageNameForMultiStageContainerfile) {
            log.warn("Detected multistage Containerfile, but there are no image names specified for (some of) these stages. Only tagging final image!");
        }

        build.validate(mavenProject, log, failOnMissingContainerfile);
    }

    @Override
    public SingleImageBuildConfiguration getBuild() {
        return build;
    }

    /**
     * Sets the {@link SingleImageBuildConfiguration} to use fot he image configuration
     *
     * @param build The build configuration to use.
     */
    public void setBuild(SingleImageBuildConfiguration build) {
        this.build = build;
    }
}
