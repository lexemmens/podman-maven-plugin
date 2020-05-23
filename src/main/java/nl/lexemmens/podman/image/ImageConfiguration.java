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
     * Constructs a new instance of this ImageConfiguration class.
     *
     * @param targetRegistry                The registry to use
     * @param tags                    The tags to build
     * @param version                 The version to use
     * @param createImageTaggedLatest Whether an image tagged 'latest' should be created
     */
    public ImageConfiguration(String targetRegistry, String[] tags, String version, boolean createImageTaggedLatest) {
        this.targetRegistry = targetRegistry;
        this.version = version;
        this.createImageTaggedLatest = createImageTaggedLatest;
        this.tags = Objects.requireNonNullElseGet(tags, () -> new String[0]);
    }

    /**
     * Returns a list of all full image names for this image. As an image can have more then one tag, this will
     * result in multiple image names.
     * <p>
     * Examples:
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

            if(createImageTaggedLatest) {
                imageNames.add(buildImageName(tag, LATEST));
            }
        }

        return imageNames;
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
        if(tags.length == 0) {
            throw new MojoExecutionException("Tags cannot be empty!");
        }

        if(version == null && !createImageTaggedLatest) {
            throw new MojoExecutionException("Cannot create image without a valid version!");
        }
    }

    /**
     * Returns an Optional of the image hash
     */
    public final Optional<String> getImageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * Sets the image hash to a specific value
     */
    public final void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public final String getRegistry() {
        String registryToReturn;

        if(targetRegistry == null && tags.length > 0) {
            registryToReturn = getRegistryFromString(tags[0]);
        } else {
            registryToReturn = targetRegistry;
        }

        return registryToReturn;
    }

    private String getRegistryFromString(String value) {
        String registryFromValue = null;
        if(value != null) {
            Matcher matcher = REGISTRY_REGEX.matcher(value);
            if(matcher.find()) {
                registryFromValue = matcher.group();
            }
        }

        return registryFromValue;
    }
}
