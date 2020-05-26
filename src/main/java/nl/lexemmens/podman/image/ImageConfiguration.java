package nl.lexemmens.podman.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Holds the configuration for the container images that are being built. Values of this class will be set via
 * the Maven pom, except for the image hash.
 */
public class ImageConfiguration {

    /**
     * The name of the image without the target registry. May contain the repository. Must be all lowercase and no special characters
     */
    @Parameter(required = true)
    protected String name;

    @Parameter
    protected BuildImageConfiguration build;

    /**
     * Set after the image is built.
     */
    private String imageHash;

    /**
     * <p>
     * Constructor
     * </p>
     */
    public ImageConfiguration() {
        // Empty - will be injected
    }

    /**
     * <p>
     * Returns an Optional that may or may not hold the image hash
     * </p>
     *
     * @return An {@link Optional} that may hold the image hash
     */
    public final Optional<String> getImageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * <p>
     * Sets the image hash to a specific value
     * </p>
     *
     * @param imageHash The image hash to set. This should be a SHA256 hash.
     */
    public final void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    /**
     * <p>
     * Returns the build configuration
     * </p>
     *
     * @return the configuration used for building the image
     */
    public BuildImageConfiguration getBuild() {
        return build;
    }

    /**
     * <p>
     * Returns a list of image names formatted as the image name [colon] tag.
     * </p>
     * <p>
     * Note that registry information is not prepended to this image name
     * </p>
     *
     * @return A list of image names
     */
    public List<String> getImageNames() {
        List<String> imageNames = new ArrayList<>();

        for (String tag : build.getAllTags()) {
            imageNames.add(String.format("%s:%s", name, tag));
        }

        return imageNames;
    }

    /**
     * <p>
     * Returns the name of the image without the tag and registry
     * </p>
     *
     * @return The name of the image
     */
    public String getImageName() {
        return name;
    }

    /**
     * Initializes this configuration and fills any null values with default values.
     *
     * @param mavenProject The MavenProject to derive some of the values from
     * @param log          The log for logging any errors that occur during validation
     * @throws MojoExecutionException In case validation fails.
     */
    public void initAndValidate(MavenProject mavenProject, Log log) throws MojoExecutionException {
        if (name == null) {
            String msg = "Image name must not be null, must be alphanumeric and may contain slashes, such as: valid/image/name";
            log.error(msg);
            throw new MojoExecutionException(msg);
        }

        build.validate(mavenProject, log);
    }
}
