package nl.lexemmens.podman.config.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.*;

public abstract class AbstractImageConfiguration<T extends AbstractImageBuildConfiguration> {

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
     * <p>
     * Returns the build configuration
     * </p>
     *
     * @return the configuration used for building the image
     */
    public abstract T getBuild();

    /**
     * Set after the image is built.
     */
    private String finalImageHash;

    /**
     * Stores the image hashes per stage in case of a multi stage Containerfile
     */
    private final Map<String, String> imageHashPerStage = new HashMap<>();

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

        for (String tag : getBuild().getAllTags()) {
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
                for (String tag : getBuild().getAllTags()) {
                    imageNames.add(String.format("%s:%s", stage.getImageName(), tag));
                }
            }
        }

        return imageNames;
    }

    /**
     * Initialises and validates this configuration
     *
     * @param log The log class for logging
     * @throws MojoExecutionException In case validation fails.
     */
    public void initAndValidate(Log log) throws MojoExecutionException {
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
     * Returns a boolean indicating whether this configuration is valid
     *
     * @return true if this configuration is valid. False otherwise.
     */
    public boolean isValid() {
        return getBuild().isValid();
    }

    /**
     * Sets the name of this image.
     *
     * @param name The name of this image
     */
    public void setImageName(String name) {
        this.name = name;
    }

    /**
     * Sets whether a custom image name should be used for each stage in the Containerfile.
     *
     * @param customImageNameForMultiStageContainerfile true if a custom name should be used for each stage in the Containerfile.
     */
    public void setCustomImageNameForMultiStageContainerfile(boolean customImageNameForMultiStageContainerfile) {
        this.customImageNameForMultiStageContainerfile = customImageNameForMultiStageContainerfile;
    }

    /**
     * Sets the custom {@link StageConfiguration}s
     *
     * @param stages The {@link StageConfiguration}s to set.
     */
    public void setStages(StageConfiguration[] stages) {
        this.stages = stages;
    }
}
