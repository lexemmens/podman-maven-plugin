package nl.lexemmens.podman;

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
     */
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    /**
     * Location of the files - usually the project's target folder
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "podman.outputdir", required = true)
    protected File outputDirectory;

    /**
     * The tag version to use. Defaults to the project version. The value of this property takes precedence over versions that are specified in the
     * tags
     */
    @Parameter(property = "podman.image.tag.version", required = true, defaultValue = "${project.version}")
    protected String tagVersion;

    /**
     * Array consisting of one or more tags to attach to a container image
     */
    @Parameter(property = "podman.image.tag")
    protected String[] tags;

    /**
     * Location of project sources
     */
    @Parameter(property = "podman.basedir", required = true, defaultValue = "${project.basedir}")
    protected File sourceDirectory;


    // Default registry to use if no registry is specified
    @Parameter(property = "podman.registry")
    protected String registry;

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
}
