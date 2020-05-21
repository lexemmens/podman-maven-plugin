package nl.lexemmens.podman.config;

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

    private final String registry;
    private final String repository;
    private final String[] tags;
    private final String version;
    private final boolean createImageTaggedLatest;

    private String imageHash;

    /**
     * Constructs a new instance of this ImageConfiguration class.
     *
     * @param registry                The registry to use
     * @param repository              The repository to use
     * @param tags                    The tags to build
     * @param version                 The version to use
     * @param createImageTaggedLatest Whether an image tagged 'latest' should be created
     */
    public ImageConfiguration(String registry, String repository, String[] tags, String version, boolean createImageTaggedLatest) {
        this.registry = registry;
        this.repository = repository;
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
        List<String> imageNames = new ArrayList<>();

        for (String tag : tags) {
            addFullImageName(imageNames, tag, version);

            if(createImageTaggedLatest) {
                addFullImageName(imageNames, tag, LATEST);
            }
        }

        return imageNames;
    }

    private void addFullImageName(List<String> imageNames, String tag, String versionToUse) throws MojoExecutionException {
        String imageName = buildImageName(tag, versionToUse);
        validateImageName(imageName);

        imageNames.add(imageName);
    }

    private String buildImageName(String tag, String versionToUse) throws MojoExecutionException {
        StringBuilder sb = new StringBuilder();
        if (registry != null) {
            sb.append(registry).append(SLASH);
        }

        if (repository != null) {
            sb.append(repository).append(SLASH);
        }

        if (tag == null) {
            throw new MojoExecutionException("Image tag cannot be empty!");
        } else {
            sb.append(tag).append(":").append(versionToUse);
        }


        return sb.toString();
    }

    private void validateImageName(String imageName) throws MojoExecutionException {
        Pattern tagPattern = Pattern.compile("^(.+?)(?::([^:/]+))?$");
        Matcher matcher = tagPattern.matcher(imageName);
        if (!matcher.matches()) {
            throw new MojoExecutionException(imageName + " is not a proper image name ([registry/][repo][:port]");
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

        if(registry == null) {
            registryToReturn = getRegistryFromString(repository);
            if(registryToReturn == null && tags.length > 0) {
                registryToReturn = getRegistryFromString(tags[0]);
            }
        } else {
            registryToReturn = registry;
        }

        return registryToReturn;
    }

    private String getRegistryFromString(String value) {
        String registryFromValue = null;
        if(value != null) {
            Matcher matcher = REGISTRY_REGEX.matcher(value);
            while(matcher.find()) {
                registryFromValue = matcher.group();
                break;
            }
        }

        return registryFromValue;
    }
}
