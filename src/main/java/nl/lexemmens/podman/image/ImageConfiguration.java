package nl.lexemmens.podman.image;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the configuration for the container images that are being built
 */
public class ImageConfiguration {

    private static final Pattern REGISTRY_REGEX = Pattern.compile("^(?:https?:\\/\\/)?(?:[^@\\n]+@)?(?:www\\.)?([^:\\/\\n?]+)([:0-9]?){0,6}");

    private static final String SLASH = "/";
    private static final String LATEST = "latest";

    private final String targetRegistry;
    private final String[] tags;
    private final String version;
    private final boolean createImageTaggedLatest;

    private String imageHash;

    /**
     * <p>
     * Constructs a new instance of this ImageConfiguration class.
     * </p>
     *
     * @param targetRegistry          The target registry where images will be pushed to. Used for tagging, pushing and saving images.
     * @param tags                    The tags to apply to the container images
     * @param version                 The version used to tag the container images with
     * @param createImageTaggedLatest Whether an image tagged 'latest' should be created
     */
    public ImageConfiguration(String targetRegistry, String[] tags, String version, boolean createImageTaggedLatest) {
        this.targetRegistry = targetRegistry;
        this.version = version;
        this.createImageTaggedLatest = createImageTaggedLatest;
        this.tags = Objects.requireNonNullElseGet(tags, () -> new String[0]);
    }

    /**
     * <p>
     * Returns a list of all full image names for this image. As an image can have more then one tag, this will
     * result in multiple image names.
     * </p>
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li>docker.consol.de:5000/jolokia/tomcat-8.0:8.0.9</li>
     * <li>docker.consol.de:5000/jolokia/tomcat-8.0:latest</li>
     * </ul>
     *
     * @return A List of all image names
     * @throws MojoExecutionException When, due to a confguration issue, the image name is invalid.
     */
    public List<String> getFullImageNames() throws MojoExecutionException {
        validateProperties();

        List<String> imageNames = new ArrayList<>();

        for (String tag : tags) {
            imageNames.add(buildImageName(tag, version));

            if (createImageTaggedLatest) {
                imageNames.add(buildImageName(tag, LATEST));
            }
        }

        return imageNames;
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
     * Returns a String representing the target registry. This may either be specified via
     * the targetRegistry parameter or be part of a tag.
     * </p>
     *
     * @return The target registry.
     */
    public final String getTargetRegistry() {
        String registryToReturn;

        if (targetRegistry == null && tags.length > 0) {
            registryToReturn = getRegistryFromString(tags[0]);
        } else {
            registryToReturn = targetRegistry;
        }

        return registryToReturn;
    }

    private String getRegistryFromString(String value) {
        String registryFromValue = null;
        Matcher matcher = REGISTRY_REGEX.matcher(value);
        if (matcher.find()) {
            registryFromValue = matcher.group();
        }
        return registryFromValue;
    }

    private String buildImageName(String tag, String versionToUse) {
        StringBuilder sb = new StringBuilder();
        if (targetRegistry != null) {
            sb.append(targetRegistry).append(SLASH);
        }

        sb.append(tag).append(":").append(versionToUse);

        return sb.toString();
    }

    private void validateProperties() throws MojoExecutionException {
        if (tags.length == 0) {
            throw new MojoExecutionException("Tags cannot be empty!");
        }

        if (version == null && !createImageTaggedLatest) {
            throw new MojoExecutionException("Cannot create image without a valid version!");
        }
    }
}
