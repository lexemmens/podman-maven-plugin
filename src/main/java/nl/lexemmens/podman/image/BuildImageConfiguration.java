package nl.lexemmens.podman.image;

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

public class BuildImageConfiguration {

    private static final String DEFAULT_DOCKERFILE = "Dockerfile";

    @Parameter
    protected boolean noCache;

    /**
     * Array consisting of one or more tags to attach to a container image.
     * Tags will be appended at the end of the image name
     */
    @Parameter
    protected String[] tags;

    /**
     * Name of the Dockerfile to use. Defaults to Dockerfile
     */
    @Parameter
    protected String dockerFile;

    /**
     * Directory containing the Dockerfile
     */
    @Parameter
    protected File dockerFileDir;

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
     * Will be set when this class is validated using the #initAndValidate() method
     */
    private File outputDirectory;

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
     * Returns the directory containing the original raw Dockerfile
     *
     * @return A {@link File} object referencing the location of the Dockerfile
     */
    public Path getSourceDockerfile() {
        Path dockerFileDirPath = Paths.get(dockerFileDir.toURI());
        return dockerFileDirPath.resolve(dockerFile);
    }

    /**
     * Returns the path to the target Dockerfile
     *
     * @return Returns a path to the target Dockerfile
     */
    public Path getTargetDockerfile() {
        return Paths.get(outputDirectory.toURI()).resolve(dockerFile);
    }

    /**
     * <p>
     * Returns the labels to be applied to the container image
     * </p>
     * <p>
     * All specified labels will be added to the Dockerfile after filtering.
     * </p>
     *
     * @return All labels to be added to the Dockerfile
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
     * Validates this class by giving all null properties a default value.
     *
     * @param project The MavenProject used to derive some of the default values from.
     * @param log     Access to Maven's log system for writing errors
     * @throws MojoExecutionException In case there is no Dockerfile at the specified source location or the Dockerfile is empty
     */
    public void validate(MavenProject project, Log log) throws MojoExecutionException {
        if (dockerFile == null) {
            dockerFile = DEFAULT_DOCKERFILE;
        }

        if (dockerFileDir == null) {
            dockerFileDir = new File("");
        }

        if (labels == null) {
            labels = new HashMap<>();
        }

        this.mavenProjectVersion = project.getVersion();
        this.outputDirectory = new File(project.getBuild().getDirectory());

        Path sourceDockerfile = getSourceDockerfile();
        if (!Files.exists(sourceDockerfile)) {
            String msg = "No Dockerfile found at " + sourceDockerfile + ". Check your the dockerFileDir and dockerFile parameters in the configuration.";
            log.error(msg);
            throw new MojoExecutionException(msg);
        }

        if (isDockerfileEmpty(log, sourceDockerfile)) {
            String msg = "The specified Dockerfile at " + sourceDockerfile + " is empty!";
            log.error(msg);
            throw new MojoExecutionException(msg);
        }
    }

    private boolean isDockerfileEmpty(Log log, Path fullDockerFilePath) throws MojoExecutionException {
        try {
            return 0 == Files.size(fullDockerFilePath);
        } catch (IOException e) {
            String msg = "Unable to determine if Dockerfile is empty.";
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }
}
