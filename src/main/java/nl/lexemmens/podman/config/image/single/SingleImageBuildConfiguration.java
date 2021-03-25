package nl.lexemmens.podman.config.image.single;

import nl.lexemmens.podman.config.image.AbstractImageBuildConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Holds the configuration for a single image
 */
public class SingleImageBuildConfiguration extends AbstractImageBuildConfiguration {

    /**
     * Constructor
     */
    public SingleImageBuildConfiguration() {
        // Empty - will be injected
    }

    /**
     * Validates this class by giving all null properties a default value.
     *
     * @param project                    The MavenProject used to derive some of the default values from.
     * @param log                        Access to Maven's log system for writing errors
     * @param failOnMissingContainerfile Whether an exception should be thrown if no Containerfile is found
     * @throws MojoExecutionException In case there is no Containerfile at the specified source location or the Containerfile is empty
     */
    public void validate(MavenProject project, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        super.validate(project);

        Path sourceContainerFile = getSourceContainerFileDir();
        if (!Files.exists(sourceContainerFile) && failOnMissingContainerfile) {
            String msg = "No Containerfile found at " + sourceContainerFile + ". Check your the containerFileDir and containerFile parameters in the configuration.";
            log.error(msg);
            throw new MojoExecutionException(msg);
        } else if (!Files.exists(sourceContainerFile)) {
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

    /**
     * Returns the directory containing the original raw Containerfile
     *
     * @return A {@link File} object referencing the location of the Containerfile
     */
    public Path getSourceContainerFileDir() {
        Path containerFileDirPath = Paths.get(containerFileDir.toURI());
        return containerFileDirPath.resolve(containerFile);
    }


}
