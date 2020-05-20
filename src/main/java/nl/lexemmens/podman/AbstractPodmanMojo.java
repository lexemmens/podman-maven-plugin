package nl.lexemmens.podman;

import nl.lexemmens.podman.config.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHubFactory;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractPodmanMojo extends AbstractMojo {

    protected static final Path DOCKERFILE = Paths.get("Dockerfile");

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Maven settings containing authentication information
     * TODO Figure this out
     */
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    /**
     * Location of the files - usually the project's target folder
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "podman.outputdir", required = true)
    protected File outputDirectory;

    /**
     * Directory containing the Dockerfile
     */
    @Parameter(property = "podman.dockerfile.dir", required = true, defaultValue = "${project.basedir}")
    protected File dockerFileDir;

    /**
     * The registry of the container images
     */
    @Parameter(property = "podman.image.registry")
    protected String registry;

    /**
     * The repository containing the container images
     */
    @Parameter(property = "podman.image.repository")
    protected String repository;

    /**
     * Array consisting of one or more tags to attach to a container image
     */
    @Parameter(property = "podman.image.tags")
    protected String[] tags;

    /**
     * Specifies whether the version of the container image should be based on the version of this Maven project. Defaults to true.
     * When set to false, 'podman.image.tag.version' must be specified.
     */
    @Parameter(property = "podman.image.version.fromMavenProject", required = true, defaultValue = "true")
    protected boolean useMavenProjectVersion;

    /**
     * Specifies the version of the container image. If specified, this parameter takes precedence over 'podman.image.tag.fromMavenProject'
     */
    @Parameter(property = "podman.image.version")
    protected String tagVersion;

    /**
     * Specified whether a container image should *ALSO* be tagged 'latest'. This defaults to false.
     */
    @Parameter(property = "podman.image.tag.latest", defaultValue = "false", required = true)
    protected boolean createLatestTag;

    /**
     * Skip all podman steps
     */
    @Parameter(property = "podman.skip", defaultValue = "false")
    private boolean skip;

    @Component
    private MavenFileFilter mavenFileFilter;

    @Component
    private ServiceHubFactory serviceHubFactory;

    @Override
    public final void execute() throws MojoExecutionException {
        if(skip) {
            getLog().info("Podman actions are skipped.");
            return;
        }

        ServiceHub hub = serviceHubFactory.createServiceHub(getLog(), mavenFileFilter);
        executeInternal(hub);
    }

    public abstract void executeInternal(ServiceHub hub) throws MojoExecutionException;

    protected ImageConfiguration getImageConfiguration() throws MojoExecutionException {
        String version;
        if(useMavenProjectVersion) {
            version = project.getVersion();
        } else if(tagVersion != null) {
            version = tagVersion;
        } else {
            String msg = "No image version specified. Set 'podman.image.version.fromMavenProject' to true for default project version or" +
                    " specify a version via 'podman.image.version'";
            getLog().error(msg);
            throw new MojoExecutionException(msg);
        }

        return new ImageConfiguration(registry, repository, tags, version, createLatestTag);
    }
}
