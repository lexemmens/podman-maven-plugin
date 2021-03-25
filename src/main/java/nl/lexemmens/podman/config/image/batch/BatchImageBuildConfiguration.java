package nl.lexemmens.podman.config.image.batch;

import nl.lexemmens.podman.config.image.AbstractImageBuildConfiguration;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains the configuration for batch image processing.
 */
public class BatchImageBuildConfiguration extends AbstractImageBuildConfiguration {

    /**
     * Takes the {@link #containerFileDir} and looks for Containerfiles in that directory.
     *
     * @return A collection of Containefiles found. May be an empty list, but is never <code>null</code>
     * @throws MojoExecutionException In case an IOException occurs during the search.
     */
    public List<Path> getAllContainerFiles() throws MojoExecutionException {
        List<Path> allContainerFiles;
        try (Stream<Path> pathStream = Files.walk(Paths.get(containerFileDir.toURI()))) {
            allContainerFiles = pathStream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(containerFile))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find Containerfiles with name '" + containerFile + "' in directory " + containerFileDir, e);
        }

        return allContainerFiles;
    }

    /**
     * Returns all configured tags
     *
     * @return all tags, may be <code>null</code>
     */
    protected String[] getTags() {
        return tags;
    }

    /**
     * Returns whether the images should be tagged with the Maven Project version.
     *
     * @return true in case the image should be tagged with the Maven Project version. False otherwise.
     */
    protected boolean isTagWithMavenProjectVersion() {
        return tagWithMavenProjectVersion;
    }
}
