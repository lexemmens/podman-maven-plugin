package nl.lexemmens.podman.context;

import nl.lexemmens.podman.config.ImageConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Context class holding all related information for building container images
 */
public class BuildContext {

    private final MavenProject mavenProject;

    private final Path sourceDockerfile;
    private final Path targetDockerfile;
    private final Log log;
    private final ImageConfiguration imageConfiguration;

    /**
     * Constructs a new instance of this context class.
     *
     * @param sourceDockerfile The source Dockerfile
     * @param targetDockerfile The target Dockerfile
     * @param log              Access to the logger
     * @param mavenProject     The Maven project
     */
    public BuildContext(Path sourceDockerfile, Path targetDockerfile, Log log, MavenProject mavenProject, ImageConfiguration imageConfiguration) {
        this.sourceDockerfile = sourceDockerfile;
        this.targetDockerfile = targetDockerfile;
        this.log = log;
        this.mavenProject = mavenProject;
        this.imageConfiguration = imageConfiguration;
    }

    /**
     * Validates this BuildContext by ensuring that:
     * <ul>
     *     <li>The source Dockerfile exists</li>
     *     <li>The source Dockerfile is not empty</li>
     * </ul>
     *
     * @throws MojoExecutionException In case validation fails.
     */
    public void validate() throws MojoExecutionException {
        if (!Files.exists(sourceDockerfile)) {
            log.info("Project does not have a Dockerfile");
            throw new MojoExecutionException("Project does not have a Dockerfile!");
        }

        if (isDockerfileEmpty(sourceDockerfile)) {
            throw new MojoExecutionException("Dockerfile cannot be empty!");
        }
    }

    private boolean isDockerfileEmpty(Path fullDockerFilePath) throws MojoExecutionException {
        try {
            return 0 == Files.size(fullDockerFilePath);
        } catch (IOException e) {
            String msg = "Unable to determine if Dockerfile is empty.";
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }

    /**
     * Returns the path to the source Dockerfile
     */
    public Path getSourceDockerfile() {
        return sourceDockerfile;
    }

    /**
     * Returns the path to the target Dockerfile
     */
    public Path getTargetDockerfile() {
        return targetDockerfile;
    }

    /**
     * Returns a reference to the MavenProject
     */
    public MavenProject getMavenProject() {
        return mavenProject;
    }

    public ImageConfiguration getImageConfiguration() {
        return imageConfiguration;
    }
}
