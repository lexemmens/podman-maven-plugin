package nl.lexemmens.podman.config.image;

import nl.lexemmens.podman.enumeration.ContainerFormat;
import nl.lexemmens.podman.enumeration.PullPolicy;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.lexemmens.podman.enumeration.ContainerFormat.OCI;

/**
 * Contains the shared configuration used by both single image configurations as well
 * as batch configurations.
 */
public abstract class AbstractImageBuildConfiguration {

    /**
     * This is the regular expression to be used to determine a multistage Containerfile. For now we only support
     * named stages.
     */
    protected static final Pattern MULTISTAGE_CONTAINERFILE_REGEX = Pattern.compile(
            "(FROM\\s[a-zA-Z0-9./:_\\-${}]{0,255}\\s)([ASas]{2}\\s)([a-zA-Z0-9./:_\\-]{1,128})"
    );

    /**
     * The default name of the Containerfile to build.
     */
    protected static final String DEFAULT_CONTAINERFILE = "Containerfile";

    /**
     * Directory containing the Containerfile
     */
    @Parameter
    protected File containerFileDir;

    /**
     * Configures whether caching should be used to build images.
     */
    @Parameter
    protected boolean noCache;

    /**
     * Configures whether from-images should be pulled so that the image will
     * always be build on the latest base.
     */
    @Parameter(defaultValue = "missing")
    protected PullPolicy pullPolicy;

    /**
     * Array consisting of one or more tags to attach to a container image.
     * Tags will be appended at the end of the image name
     */
    @Parameter
    protected String[] tags;

    /**
     * Name of the Containerfile to use. Defaults to Containerfile
     */
    @Parameter
    protected String containerFile;

    /**
     * Collection of build arguments to use during Podman build
     */
    @Parameter
    private Map<String, String> args;

    /**
     * Specify any labels to be applied to the image
     */
    @Parameter
    protected Map<String, String> labels;

    /**
     * Specifies whether a version of the container image should be based on the version of this Maven project. Defaults to true.
     * When set to false, 'podman.image.tag.version' must be specified.
     */
    @Parameter
    protected boolean tagWithMavenProjectVersion;

    /**
     * The Maven project version to use (only when useMavenProjectVersion is set to true)
     */
    protected String mavenProjectVersion;

    /**
     * Specified whether a container image should *ALSO* be tagged 'latest'. This defaults to false.
     */
    @Parameter
    protected boolean createLatestTag;

    /**
     * Specifies the format of the Container image to use
     */
    @Parameter
    protected ContainerFormat format;

    /**
     * Squash all of the image’s new layers into a single new layer; any preexisting layers are not squashed.
     * <p>
     *
     * @see "http://docs.podman.io/en/latest/markdown/podman-build.1.html"
     */
    @Parameter
    protected Boolean squash;

    /**
     * Squash all of the new image’s layers (including those inherited from a base image) into a single new layer.
     * <p>
     *
     * @see "http://docs.podman.io/en/latest/markdown/podman-build.1.html"
     */
    @Parameter
    protected Boolean squashAll;

    /**
     * Cache intermediate images during the build process (Default is true).
     * <p>
     * Note: You can also override the default value of layers by setting the BUILDAH_LAYERS environment variable.
     * export BUILDAH_LAYERS=true
     *
     * @see "http://docs.podman.io/en/latest/markdown/podman-build.1.html"
     */
    @Parameter
    protected Boolean layers;

    /**
     * Specify the resulting image platform by being passed to the "--platform" option of podman.
     * <p>
     *
     * @see "https://docs.podman.io/en/latest/markdown/podman-build.1.html"
     */
    @Parameter
    protected String platform;

    /**
     * Specify the final target stage to build. It is passed to the "--target" option of podman.
     * <p>
     *
     * @see "https://docs.podman.io/en/latest/markdown/podman-build.1.html"
     */
    @Parameter
    protected String targetStage;

    /**
     * Will be set when this class is validated using the #initAndValidate() method
     */
    protected File outputDirectory;

    /**
     * Will be set to true when the Containerfile is a multistage Containerfile.
     */
    private boolean isMultistageContainerFile;

    /**
     * Represents the validity of this configuration
     */
    protected boolean valid;

    /**
     * Returns which value should be used for the --no-cache property
     *
     * @return When set to true, podman will run with --no-cache=true
     */
    public boolean isNoCache() {
        return noCache;
    }

    /**
     * Returns if the --pull property should be used
     *
     * @return When set to true, podman will build with --pull
     */
    public Optional<PullPolicy> getPullPolicy() {
        return Optional.ofNullable(pullPolicy);
    }

    public void validate(MavenProject project) {
        if (containerFile == null) {
            containerFile = DEFAULT_CONTAINERFILE;
        }

        if (labels == null) {
            labels = new HashMap<>();
        }

        if (format == null) {
            format = OCI;
        }

        this.mavenProjectVersion = project.getVersion();
        this.outputDirectory = new File(project.getBuild().getDirectory());

        if (containerFileDir == null) {
            containerFileDir = project.getBasedir();
        }

        if(args == null) {
            args = new HashMap<>();
        }
    }

    /**
     * Returns the tags to be applied for this image
     *
     * @return The tags to be applied
     */
    public List<String> getAllTags() {
        List<String> allTags = new ArrayList<>();
        if (tags != null) {
            allTags.addAll(Arrays.asList(tags));
        }

        if (createLatestTag) {
            allTags.add("latest");
        }

        if (tagWithMavenProjectVersion) {
            allTags.add(mavenProjectVersion);
        }
        return allTags;
    }

    /**
     * Returns the path to the target Containerfile
     *
     * @return Returns a path to the target Containerfile
     */
    public Path getTargetContainerFile() {
        return Paths.get(outputDirectory.toURI()).resolve(containerFile);
    }

    /**
     * <p>
     * Returns the labels to be applied to the container image
     * </p>
     * <p>
     * All specified labels will be added to the Containerfile after filtering.
     * </p>
     *
     * @return All labels to be added to the Containerfile
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Returns the project's output directory
     *
     * @return The configured output directory
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Returns the format for the built image's manifest and configuration data.
     *
     * @return The format for the built image's manifest and configuration data
     */
    public ContainerFormat getFormat() {
        return format;
    }

    /**
     * Returns true when the Containerfile is a multistage Containerfile
     *
     * @return true when a multistage Containerfile is used
     */
    public boolean isMultistageContainerFile() {
        return isMultistageContainerFile;
    }

    /**
     * Returns the Pattern that is used to determine if a line matches a multi-stage Containerfile
     *
     * @return The pattern to determine if a line matches the expected pattern for a multi-stage Containerfile.
     */
    public Pattern getMultistageContainerfilePattern() {
        return MULTISTAGE_CONTAINERFILE_REGEX;
    }

    /**
     * Returns true when all of the image’s new layers should be squashed into a single new layer; any
     * preexisting layers are not squashed.
     *
     * @return true when all of the images's new layers should be squashed into a new layer. False otherwise.
     */
    public Boolean getSquash() {
        return squash;
    }

    /**
     * Returns true when all of the image’s new layers, including those inherited from a base image, into a single new layer.
     *
     * @return true when all of the images's new layers should be squashed into a new layer. False otherwise.
     */
    public Boolean getSquashAll() {
        return squashAll;
    }

    /**
     * Returns true if intermediate layers should be cached during a Podman build.
     *
     * @return true, when intermediate layers should be cached. False otherwise.
     */
    public Boolean getLayers() {
        return layers;
    }

    /**
     * Returns the platform that Podman should be told to build for.
     *
     * @return When set, a string in the format of GO OS/GO ARCH (because GO is the language of Podman) for example: linux/arm64
     */
    public Optional<String> getPlatform(){
        return Optional.ofNullable(platform);
    }

    /**
     * Returns the final target stage to build.
     *
     * @return if set, the final target stage to build defined in Containerfile.
     */
    public Optional<String> getTargetStage(){
        return Optional.ofNullable(targetStage);
    }

    /**
     * Returns a boolean indicating whether this configuration is valid
     *
     * @return true if this configuration is valid. False otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns true when a latest tag should be created
     *
     * @return true if an image should be tagged 'latest'
     */
    public boolean isCreateLatestTag() {
        return createLatestTag;
    }

    protected boolean isContainerFileEmpty(Log log, Path fullContainerFilePath) throws MojoExecutionException {
        try {
            return 0 == Files.size(fullContainerFilePath);
        } catch (IOException e) {
            String msg = "Unable to determine if Containerfile is empty.";
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }

    protected void determineBuildStages(Log log, Path fullContainerFilePath) throws MojoExecutionException {
        boolean foundTargetStage = false;
        try (Stream<String> containerFileStream = Files.lines(fullContainerFilePath)) {
            List<String> content = containerFileStream.filter(c -> !c.matches("^\\s*#.*"))
                                                      .collect(Collectors.toList());
            for (String line : content) {
                Matcher matcher = MULTISTAGE_CONTAINERFILE_REGEX.matcher(line);
                if (matcher.find()) {
                    isMultistageContainerFile = true;

                    String stage = matcher.group(3);
                    if (Objects.equals(stage, targetStage)) {
                        foundTargetStage = true;
                    }
                    log.debug("Found a stage named: " + stage);
                }
            }
        } catch (IOException e) {
            String msg = "Unable to determine if Containerfile is a multistage Containerfile.";
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }

        if (targetStage != null  && isMultistageContainerFile && !foundTargetStage) {
            String msg = String.format("Target stage '%s' was not found in the given Containerfile.", targetStage);
            log.error(msg);
            throw new MojoExecutionException(msg);
        }
    }

    /**
     * Configures whether to cache intermediate images during the build process.
     *
     * @param layers The value to set
     */
    public void setLayers(Boolean layers) {
        this.layers = layers;
    }

    /**
     * Used to set the platform to be passed to Podman when building the image.
     *
     * @param platform String in the format of GO OS/GO ARCH (because GO is the language of Podman) for example: linux/arm64
     */
    public void setPlatform(String platform){
        this.platform = platform;
    }

    /**
     * Sets the noCache option. Allows configuring whether caching should be used
     * to cache images
     *
     * @param noCache Sets the noCache option on and off.
     */
    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    /**
     * Configures whether from-images should be pulled so that the image will
     * always be build on the latest base.
     *
     * @param pullPolicy The value to set
     */
    public void setPullPolicy(PullPolicy pullPolicy) {
        this.pullPolicy = pullPolicy;
    }

    /**
     * Configures whether to squash all newly created layers into one layer.
     *
     * @param squash The value to set
     */
    public void setSquash(Boolean squash) {
        this.squash = squash;
    }

    /**
     * Configures whether to squash all layers into one layer, this includes the base layer(s).
     *
     * @param squashAll The value to set
     */
    public void setSquashAll(Boolean squashAll) {
        this.squashAll = squashAll;
    }

    /**
     * Sets the tags that should be used for this image
     *
     * @param tags The tags this image should receive
     */
    public void setTags(String[] tags) {
        this.tags = tags;
    }

    /**
     * Sets the name of the Containerfile (defaults to Containerfile)
     *
     * @param containerFile The name of the Containerfile to set
     */
    public void setContainerFile(String containerFile) {
        this.containerFile = containerFile;
    }

    /**
     * Sets the labels to add to the container image.
     *
     * @param labels The labels to set.
     */
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    /**
     * Specifies whether the image should be tagged with the Maven Project version
     *
     * @param tagWithMavenProjectVersion whether the image should be tagged with the Maven project version
     */
    public void setTagWithMavenProjectVersion(boolean tagWithMavenProjectVersion) {
        this.tagWithMavenProjectVersion = tagWithMavenProjectVersion;
    }

    /**
     * Specifies whether a latest tag should be created
     *
     * @param createLatestTag If true, the image will receive the tag 'latest'
     */
    public void setCreateLatestTag(boolean createLatestTag) {
        this.createLatestTag = createLatestTag;
    }

    /**
     * The format of the container image to use.
     *
     * @param format The format to use
     */
    public void setFormat(ContainerFormat format) {
        this.format = format;
    }

    /**
     * Sets the directory where the Containerfile is located (copied from BatchImageBuildCOnfiguration).
     *
     * @param containerFileDir The directory to set
     */
    public void setContainerFileDir(File containerFileDir) {
        this.containerFileDir = containerFileDir;
    }

    /**
     * Sets the final target stage to build.
     *
     * @param targetStage The final target stage to build.
     */
    public void setTargetStage(String targetStage) {
        this.targetStage = targetStage;
    }

    /**
     * Retrieves a collection of build arguments to provide to podman build using --build-arg
     *
     * @return A collection of build arguments to provide to podman build
     */
    public Map<String, String> getArgs() {
        return args;
    }

    /**
     * The build arguments to set for use during Podman build
     *
     * @param args The arguments to set
     */
    public void setArgs(Map<String, String> args) {
        this.args = args;
    }
}
