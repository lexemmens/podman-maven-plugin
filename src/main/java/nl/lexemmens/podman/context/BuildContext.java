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

    private MavenProject mavenProject;

    private Path sourceDockerfile;
    private Path targetDockerfile;

    private Log log;
    private ImageConfiguration imageConfiguration;

    /**
     * Constructs an empty new instance of this context class.
     * Use the builder to set its values
     */
    BuildContext(){
        // Empty
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

    /**
     * Returns the container image configuration
     */
    public ImageConfiguration getImageConfiguration() {
        return imageConfiguration;
    }



    /**
     * Builder class that can be used to construct a new instance of the BuildContext
     */
    public static class Builder {

        /**
         * The instance to return
         */
        private BuildContext ctx = new BuildContext();

        /**
         * Returns the constructed instance
         */
        public BuildContext build() {
            return ctx;
        }

        /**
         * Sets the source Dockerfile
         * @return This builder instance
         */
        public Builder setSourceDockerFile(Path sourceDockerfile) {
            ctx.sourceDockerfile = sourceDockerfile;
            return this;
        }

        /**
         * Sets the target Dockerfile
         * @return This builder instance
         */
        public Builder setTargetDockerfile(Path targetDockerfile) {
            ctx.targetDockerfile = targetDockerfile;
            return this;
        }

        /**
         * Sets the logger reference
         * @return This builder instance
         */
        public Builder setLog(Log log) {
            ctx.log = log;
            return this;
        }

        /**
         * Sets the Maven project
         * @return This builder instance
         */
        public Builder setMavenProject(MavenProject mavenProject) {
            ctx.mavenProject = mavenProject;
            return this;
        }

        /**
         * Sets the container image configuration
         * @return This builder instance
         */
        public Builder setImageConfiguration(ImageConfiguration imageConfiguration) {
            ctx.imageConfiguration = imageConfiguration;
            return this;
        }

        /**
         * Validates the BuildContext
         * @return This builder instance
         * @throws MojoExecutionException When validation doe not succeed.
         */
        public Builder validate() throws MojoExecutionException {
            ctx.validate();
            return this;
        }
    }
}
