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
public class SingleImageConfiguration extends AbstractImageConfiguration {

    /**
     * The build image configuration.
     */
    @Parameter
    protected SingleImageBuildConfiguration build;

    /**
     * <p>
     * Returns the build configuration
     * </p>
     *
     * @return the configuration used for building the image
     */
    @Override
    public SingleImageBuildConfiguration getBuild() {
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
    @Override
    public void initAndValidate(MavenProject mavenProject, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        super.initAndValidate(mavenProject, log, failOnMissingContainerfile);
        build.validate(mavenProject, log, failOnMissingContainerfile);

        if (!customImageNameForMultiStageContainerfile && name == null) {
            String msg = "Image name must not be null, must be alphanumeric and may contain slashes, such as: valid/image/name";
            log.error(msg);
            throw new MojoExecutionException(msg);
        }

        if (customImageNameForMultiStageContainerfile && stages == null) {
            String msg = "Plugin is configured for multistage Containerfiles, but there are no custom image names configured.";
            log.error(msg);
            throw new MojoExecutionException(msg);
        }

        if (build.isMultistageContainerFile() && !customImageNameForMultiStageContainerfile) {
            log.warn("Detected multistage Containerfile, but there are no image names specified for (some of) these stages. Only tagging final image!");
        }
    }
}