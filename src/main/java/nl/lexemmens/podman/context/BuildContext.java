package nl.lexemmens.podman.context;

import nl.lexemmens.podman.image.ImageConfiguration;
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
     * <p>
     * Constructs an empty new instance of this context class.
     * </p>
     * <p>
     * Use the builder to set its values
     * </p>
     */
    BuildContext() {
        // Empty
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
     * <p>
     * Returns the path to the source Dockerfile. This is the Dockerfile that optionally may contain
     * parameters, such as Maven properties like ${property.name}
     * </p>
     * <p>
     * The source Dockerfile is usually located in the root of the <em>module</em>.
     * </p>
     *
     * @return The original Dockerfile
     */
    public Path getSourceDockerfile() {
        return sourceDockerfile;
    }

    /**
     * <p>
     * Returns the path to the target Dockerfile. This is the filtered Dockerfile, which will be
     * used to build the container images
     * </p>
     * <p>
     * The target Dockerfile is usually located in the module's target folder
     * </p>
     *
     * @return The target Dockerfile
     */
    public Path getTargetDockerfile() {
        return targetDockerfile;
    }

    /**
     * <p>
     * Returns a reference to the MavenProject
     * </p>
     *
     * @return A reference to this Maven project
     */
    public MavenProject getMavenProject() {
        return mavenProject;
    }

    /**
     * <p>
     * Returns the container image configuration
     * </p>
     *
     * @return A reference to the container image configuration.
     */
    public ImageConfiguration getImageConfiguration() {
        return imageConfiguration;
    }


    /**
     * <p>
     * Builder class that can be used to construct a new instance of the BuildContext
     * </p>
     */
    public static class Builder {

        /**
         * <p>
         * The instance to return
         * </p>
         */
        private BuildContext ctx = new BuildContext();

        /**
         * <p>
         * Returns the constructed {@link BuildContext} instance
         * </p>
         *
         * @return The constructed {@link BuildContext} instance
         */
        public BuildContext build() {
            return ctx;
        }

        /**
         * <p>
         * Sets the path to the source Dockerfile. Usually located in the root of the module.
         * </p>
         * <p>
         * This is the Dockerfile that optionally may contain parameters, such as Maven
         * properties like ${property.name}
         * </p>
         *
         * @param sourceDockerfile The path to the source Dockerfile
         * @return This builder instance
         */
        public Builder setSourceDockerFile(Path sourceDockerfile) {
            ctx.sourceDockerfile = sourceDockerfile;
            return this;
        }

        /**
         * <p>
         * Sets the target Dockerfile location. This identifies the location where the filtered
         * Dockerfile will be stored.
         * </p>
         * <p>
         * Its folder will be used to execute the podman command from
         * </p>
         *
         * @param targetDockerfile The path to the target Dockerfile
         * @return This builder instance
         */
        public Builder setTargetDockerfile(Path targetDockerfile) {
            ctx.targetDockerfile = targetDockerfile;
            return this;
        }

        /**
         * <p>
         * Sets a reference to Maven's log system
         * </p>
         *
         * @param log Maven's logger
         * @return This builder instance
         */
        public Builder setLog(Log log) {
            ctx.log = log;
            return this;
        }

        /**
         * <p>
         * Sets a reference this Maven project
         * </p>
         * <p>
         * This is usually set by Maven self
         * </p>
         *
         * @param mavenProject This MavenProject
         * @return This builder instance
         */
        public Builder setMavenProject(MavenProject mavenProject) {
            ctx.mavenProject = mavenProject;
            return this;
        }

        /**
         * <p>
         * Sets the configuration that will be used to build, save and push the
         * container images with <em>Podman</em>
         * </p>
         *
         * @param imageConfiguration The image configuration to use.
         * @return This builder instance
         */
        public Builder setImageConfiguration(ImageConfiguration imageConfiguration) {
            ctx.imageConfiguration = imageConfiguration;
            return this;
        }

        /**
         * Validates this BuildContext by ensuring that:
         * <ul>
         *     <li>The source Dockerfile exists</li>
         *     <li>The source Dockerfile is not empty</li>
         * </ul>
         *
         * @return This builder instance
         * @throws MojoExecutionException In case validation of the above mentioned steps fails.
         */
        public Builder validate() throws MojoExecutionException {
            if (!Files.exists(ctx.sourceDockerfile)) {
                ctx.log.info("Project does not have a Dockerfile");
                throw new MojoExecutionException("Project does not have a Dockerfile!");
            }

            if (ctx.isDockerfileEmpty(ctx.sourceDockerfile)) {
                throw new MojoExecutionException("Dockerfile cannot be empty!");
            }

            return this;
        }
    }
}
