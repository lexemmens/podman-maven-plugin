package nl.lexemmens.podman.image;

import nl.lexemmens.podman.enumeration.ContainerFormat;
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

public class BuildImageConfiguration {

    /**
     * This is the regular expression to be used to determine a multistage Containerfiles. For now we only support
     * named stages.
     */
    private static final Pattern MULTISTAGE_CONTAINERFILE_REGEX = Pattern.compile(".*(FROM\\s.*)([ASas]\\s)([a-zA-Z].*)");

    /**
     * The default name of the Containerfile to build.
     */
    private static final String DEFAULT_CONTAINERFILE = "Containerfile";

    /**
     * Configures whether caching should be used to build images.
     */
    @Parameter
    protected boolean noCache;

    /**
     * Configures whether from-images should be pulled so that the image will
     * always be build on the latest base.
     */
    @Parameter
    protected boolean pull;


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
     * Directory containing the Containerfile
     */
    @Parameter
    protected File containerFileDir;

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
    @Parameter
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
     * Will be set when this class is validated using the #initAndValidate() method
     */
    private File outputDirectory;

    /**
     * Will be set to true when the Containerfile is a multistage Containerfile.
     */
    private boolean isMultistageContainerFile;

    /**
     * Represents the validity of this configuration
     */
    private boolean valid;

    /**
     * List of all build stages (only populated in case of multistage Containerfile)
     */
    private List<String> stages = new ArrayList<>();


    /**
     * Constructor
     */
    public BuildImageConfiguration() {
        // Empty - will be injected
    }

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
    public boolean isPull() {
        return pull;
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
     * Returns the directory containing the original raw Containerfile
     *
     * @return A {@link File} object referencing the location of the Containerfile
     */
    public Path getSourceContainerFileDir() {
        Path containerFileDirPath = Paths.get(containerFileDir.toURI());
        return containerFileDirPath.resolve(containerFile);
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
     * Returns the location of the raw Containerfile. This location is used
     * as the context dir when running podman.
     *
     * @return The location of the raw Containerfile as a File.
     */
    public File getContainerFileDir() {
        return containerFileDir;
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
     * Returns a list of all stages present in the Containerfile
     *
     * @return a list of all stages.
     */
    public List<String> getStages() {
        return stages;
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
     * Returns a boolean indicating whether this configuration is valid
     * @return true if this configuration is valid. False otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Validates this class by giving all null properties a default value.
     *
     * @param project The MavenProject used to derive some of the default values from.
     * @param log     Access to Maven's log system for writing errors
     * @param failOnMissingContainerfile Whether an exception should be thrown if no Containerfile is found
     * @throws MojoExecutionException In case there is no Containerfile at the specified source location or the Containerfile is empty
     */
    public void validate(MavenProject project, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        if (containerFile == null) {
            containerFile = DEFAULT_CONTAINERFILE;
        }

        if (containerFileDir == null) {
            containerFileDir = project.getBasedir();
        }

        if (labels == null) {
            labels = new HashMap<>();
        }

        if (format == null) {
            format = OCI;
        }

        this.mavenProjectVersion = project.getVersion();
        this.outputDirectory = new File(project.getBuild().getDirectory());

        Path sourceContainerFile = getSourceContainerFileDir();
        if (!Files.exists(sourceContainerFile) && failOnMissingContainerfile) {
            String msg = "No Containerfile found at " + sourceContainerFile + ". Check your the containerFileDir and containerFile parameters in the configuration.";
            log.error(msg);
            throw new MojoExecutionException(msg);
        } else if(!Files.exists(sourceContainerFile)) {
            log.warn("No Containerfile was found at " + sourceContainerFile + ", however this will be ignored due to current plugin configuration.");
            valid = false;
        } else {
            if (isContainerFileEmpty(log, sourceContainerFile)) {
                String msg = "The specified Containerfile at " + sourceContainerFile + " is empty!";
                log.error(msg);
                throw new MojoExecutionException(msg);
            }

            determineBuildStages(log, sourceContainerFile);
            valid = true;
        }
    }

    private boolean isContainerFileEmpty(Log log, Path fullContainerFilePath) throws MojoExecutionException {
        try {
            return 0 == Files.size(fullContainerFilePath);
        } catch (IOException e) {
            String msg = "Unable to determine if Containerfile is empty.";
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }

    private void determineBuildStages(Log log, Path fullContainerFilePath) throws MojoExecutionException {
        try (Stream<String> containerFileStream = Files.lines(fullContainerFilePath)) {
            List<String> content = containerFileStream.collect(Collectors.toList());
            for (String line : content) {
                Matcher matcher = MULTISTAGE_CONTAINERFILE_REGEX.matcher(line);
                if (matcher.find()) {
                    isMultistageContainerFile = true;

                    String stage = matcher.group(3);

                    log.debug("Found a stage named: " + stage);
                    stages.add(stage);
                }
            }
        } catch (IOException e) {
            String msg = "Unable to determine if Containerfile is a multistage Containerfile.";
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }
}
