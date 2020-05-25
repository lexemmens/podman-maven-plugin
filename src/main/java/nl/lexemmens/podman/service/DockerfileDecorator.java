package nl.lexemmens.podman.service;

import nl.lexemmens.podman.image.ImageConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * <p>
 * FilterSupport class that brings support for accessing properties from a {@link MavenProject}. Both properties
 * specified via the properties tag in a pom file as well as the default Maven properties are supported
 * </p>
 */
public class DockerfileDecorator {

    /**
     * Logger instance
     */
    private final Log log;

    /**
     * The MavenPropertyReader that can be used to get access to default Maven properties.
     */
    private final MavenFileFilter mavenFileFilter;

    /**
     * The Maven project to retrieve the properties from
     */
    private final MavenProject mavenProject;

    /**
     * Constructs a new instance of this FilterSupport class.
     *
     * @param log             The logger
     * @param mavenFileFilter The MavenProject
     */
    public DockerfileDecorator(Log log, MavenFileFilter mavenFileFilter, MavenProject mavenProject) {
        this.log = log;
        this.mavenFileFilter = mavenFileFilter;
        this.mavenProject = mavenProject;
    }

    /**
     * <p>
     * Filters a Dockerfile by using the {@link MavenFileFilter} service to filter the file.
     * </p>
     *
     * @param image The BuildContext that contains the source and target Dockerfile paths
     * @throws MojoExecutionException When the Dockerfile cannot be filtered.
     */
    public void decorateDockerfile(ImageConfiguration image) throws MojoExecutionException {
        filterDockerfile(image);
        addLabelsToDockerfile(image);
    }

    private void filterDockerfile(ImageConfiguration image) throws MojoExecutionException {
        log.debug("Filtering Dockerfile. Source: " + image.getBuild().getSourceDockerfile() + ", target: " + image.getBuild().getTargetDockerfile());
        try {
            MavenFileFilterRequest fileFilterRequest = new MavenFileFilterRequest();
            fileFilterRequest.setEncoding("UTF8");
            fileFilterRequest.setFiltering(true);
            fileFilterRequest.setFrom(image.getBuild().getSourceDockerfile().toFile());
            fileFilterRequest.setTo(image.getBuild().getTargetDockerfile().toFile());
            fileFilterRequest.setMavenProject(mavenProject);

            mavenFileFilter.copyFile(fileFilterRequest);
        } catch (MavenFilteringException e) {
            String msg = "Failed to filter Dockerfile! " + e.getMessage();
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }

    private void addLabelsToDockerfile(ImageConfiguration image) throws MojoExecutionException {
        if (image.getBuild().getLabels().isEmpty()) {
            log.debug("No labels to add to the Dockerfile");
            return;
        }

        StringBuilder labelBuilder = new StringBuilder("LABEL ");
        for (Map.Entry<String, String> label : image.getBuild().getLabels().entrySet()) {
            labelBuilder.append(label.getKey()).append("=").append(label.getValue()).append(" ");
        }
        String targetLabels = labelBuilder.toString();

        try {
            Files.write(image.getBuild().getTargetDockerfile(), targetLabels.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            String msg = "Failed to add labels (" + targetLabels + ") to Dockerfile: " + e.getMessage();
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }
}
