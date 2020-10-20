package nl.lexemmens.podman.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.*;

/**
 * Holds the configuration for the container images that are being built. Values of this class will be set via
 * the Maven pom, except for the image hash.
 */
public class ImageConfiguration {

    /**
     * The name of the image without the target registry. May contain the repository. Must be all lowercase and no special characters.
     * Must be required when customImageNameForMultiStageContainerfile is set to false or not specified.
     */
    @Parameter
    protected String name;

    /**
     * When set to true
     */
    @Parameter(defaultValue = "false")
    protected boolean customImageNameForMultiStageContainerfile;

    /**
     * Allows specifying a custom image name per stage in case of a multistage Containerfile. When customImageNameForMultiStageContainerfile is set to true
     * this must be specified.
     */
    @Parameter
    protected StageConfiguration[] stages;

    /**
     * The build image configuration.
     */
    @Parameter
    protected BuildImageConfiguration build;

    /**
     * Set after the image is built.
     */
    private String finalImageHash;

    /**
     * Stores the image hashes per stage in case of a multi stage Containerfile
     */
    private Map<String, String> imageHashPerStage = new HashMap<>();

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
    public final Optional<String> getFinalImageHash() {
        return Optional.ofNullable(finalImageHash);
    }

    /**
     * Returns a Map containing all image hashes per stage as listed in the Containerfile
     *
     * @return Returns a Map of the final image hash for each stage.
     */
    public final Map<String, String> getImageHashPerStage() {
        return imageHashPerStage;
    }

    /**
     * <p>
     * Sets the image hash to a specific value
     * </p>
     *
     * @param finalImageHash The image hash to set. This should be a SHA256 hash.
     */
    public final void setFinalImageHash(String finalImageHash) {
        this.finalImageHash = finalImageHash;
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
     * Returns whether a custom image name per stage should be used (when using a multistage Containerfile).
     *
     * @return true when certain stages in a multistage Containerfile should have unique names.
     */
    public boolean useCustomImageNameForMultiStageContainerfile() {
        return customImageNameForMultiStageContainerfile;
    }

    /**
     * Returns the stage configuration for naming images
     *
     * @return the configuration for naming images when using a multistage Containerfile.
     */
    public StageConfiguration[] getStages() {
        return stages;
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
     * Returns a list of image names (without the registry) for a specific stage name. The list that is returned
     * is based on the tags that are configured.
     *
     * @param stageName The name of the stage to retrieve the image name for
     * @return A list of image names for the specific stage.
     */
    public List<String> getImageNamesByStage(String stageName) {
        List<String> imageNames = new ArrayList<>();

        for (StageConfiguration stage : stages) {
            if (stageName.equals(stage.getName())) {
                for (String tag : build.getAllTags()) {
                    imageNames.add(String.format("%s:%s", stage.getImageName(), tag));
                }
            }
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
        build.validate(mavenProject, log);

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
