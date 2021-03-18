package nl.lexemmens.podman.config.image.batch;

import nl.lexemmens.podman.config.image.AbstractImageBuildConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageBuildConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class BatchImageBuildConfiguration extends AbstractImageBuildConfiguration {

    public List<Path> getAllContainerFiles() throws MojoExecutionException {
        List<Path> allContainerFiles;
        try {
            allContainerFiles = Files.walk(Paths.get(containerFileDir.toURI()))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(containerFile))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find Containerfiles with name '" + containerFile + "'in directory " + containerFileDir, e);
        }

        return allContainerFiles;
    }

    public File getContainerFileDir() {
        return containerFileDir;
    }

    public void setContainerFileDir(File path) {
        this.containerFileDir = path;
    }

    protected String[] getTags() {
        return tags;
    }

    protected boolean isTagWithMavenProjectVersion() {
        return tagWithMavenProjectVersion;
    }
}
