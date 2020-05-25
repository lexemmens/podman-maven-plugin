package nl.lexemmens.podman.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BuildImageConfiguration {

    private static final String DEFAULT_DOCKERFILE = "Dockerfile";

    @Parameter(property = "podman.nocache")
    private boolean noCache;

    /**
     * Array consisting of one or more tags to attach to a container image.
     * Tags will be appended at the end of the image name
     */
    @Parameter(property = "podman.image.tags")
    private String[] tags;

    /**
     * Name of the Dockerfile to use. Defaults to Dockerfile
     */
    @Parameter(property = "podman.dockerfile")
    private String dockerFile;

    /**
     * Directory containing the Dockerfile
     */
    @Parameter(property = "podman.dockerfile.dir", defaultValue = "${project.basedir}")
    private File dockerFileDir;

    /**
     * Specify any labels to be applied to the image
     */
    @Parameter(property = "podman.image.labels")
    private Map<String, String> labels;

    /**
     * Specifies whether the version of the container image should be based on the version of this Maven project. Defaults to true.
     * When set to false, 'podman.image.tag.version' must be specified.
     */
    @Parameter(property = "podman.image.tag.maven")
    private boolean useMavenProjectVersion;

    /**
     * The Maven project version to use (only when useMavenProjectVersion is set to true)
     */
    @Parameter(property = "")
    private String mavenProjectVersion;

    /**
     * Specified whether a container image should *ALSO* be tagged 'latest'. This defaults to false.
     */
    @Parameter(property = "podman.image.tag.latest", defaultValue = "false", required = true)
    private boolean createLatestTag;

    /**
     * Will be set by
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

        if (useMavenProjectVersion) {
            allTags.add(mavenProjectVersion);
        }
        return allTags;
    }

    /**
     * Returns the name of the Dockerfile to be used
     *
     * @return The name of the Dockerfile to be used. Returns 'Dockerfile' when not explicitly configured
     */
    public String getDockerFile() {
        return dockerFile;
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
     * @return
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
     * Specifies whether a tag should be added with the current version of the Maven project
     *
     * @return Tags the image with the current version of the Maven project
     */
    public boolean isUseMavenProjectVersion() {
        return useMavenProjectVersion;
    }

    /**
     * Specifies whether an image should get a tag 'latest'
     *
     * @return Tags an image with 'latest' when set to true.
     */
    public boolean isCreateLatestTag() {
        return createLatestTag;
    }

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

        if (mavenProjectVersion == null) {
            mavenProjectVersion = project.getVersion();
        }

        this.outputDirectory = new File(project.getBuild().getDirectory());
    }
}
