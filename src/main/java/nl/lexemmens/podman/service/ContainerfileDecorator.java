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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Class that brings support for decorating Dockerfiles. This means that it can:
 * </p>
 * <ul>
 *     <li>Decorate a specified source Dockerfile by resolving properties configured in the {@link MavenProject}</li>
 *     <li>Decorate a specified source Dockerfile by adding one or more label commands to the Dockerfile</li>
 * </ul>
 *
 * <p>
 *     With respect to properties: Both properties specified via the properties tag in a pom
 *     file as well as the default Maven properties are supported
 * </p>
 */
public class ContainerfileDecorator {

    private static final String LABEL_ATTRIBUTE = "LABEL ";
    private static final String BASE_IMAGE_ATTRIBUTE = "FROM";

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
     * @param mavenFileFilter Maven's File Filtering service
     * @param mavenProject    The MavenProject
     */
    public ContainerfileDecorator(Log log, MavenFileFilter mavenFileFilter, MavenProject mavenProject) {
        this.log = log;
        this.mavenFileFilter = mavenFileFilter;
        this.mavenProject = mavenProject;
    }

    /**
     * <p>
     * Decorates a Dockerfile by executing a series of actions to get to the final Dockerfile:
     * </p>
     * <ul>
     *     <li>Use the {@link MavenFileFilter} service to filter the source Dockerfile and copy it to the target location.</li>
     *     <li>Add labels to the target Dockerfile using the LABELS command.</li>
     * </ul>
     *
     * @param image The BuildContext that contains the source and target Dockerfile paths
     * @throws MojoExecutionException When the Dockerfile cannot be filtered.
     */
    public void decorateContainerfile(ImageConfiguration image) throws MojoExecutionException {
        filterContainerfile(image);
        addLabelsToContainerfile(image);
    }

    private void filterContainerfile(ImageConfiguration image) throws MojoExecutionException {
        log.debug("Filtering Containerfile. Source: " + image.getBuild().getSourceContainerFileDir() + ", target: " + image.getBuild().getTargetContainerFile());
        try {
            MavenFileFilterRequest fileFilterRequest = new MavenFileFilterRequest();
            fileFilterRequest.setEncoding("UTF8");
            fileFilterRequest.setFiltering(true);
            fileFilterRequest.setFrom(image.getBuild().getSourceContainerFileDir().toFile());
            fileFilterRequest.setTo(image.getBuild().getTargetContainerFile().toFile());
            fileFilterRequest.setMavenProject(mavenProject);

            mavenFileFilter.copyFile(fileFilterRequest);
        } catch (MavenFilteringException e) {
            String msg = "Failed to filter Containerfile! " + e.getMessage();
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }

    private void addLabelsToContainerfile(ImageConfiguration image) throws MojoExecutionException {
        if (image.getBuild().getLabels().isEmpty()) {
            log.debug("No labels to add to the Containerfile");
            return;
        }

        StringBuilder labelBuilder = new StringBuilder(LABEL_ATTRIBUTE);
        for (Map.Entry<String, String> label : image.getBuild().getLabels().entrySet()) {
            labelBuilder.append(label.getKey()).append("=").append(label.getValue()).append(" ");
        }
        String targetLabels = labelBuilder.toString();

        try (Stream<String> containerFileStream = Files.lines(image.getBuild().getTargetContainerFile())) {
            List<String> containerFileContents = containerFileStream.collect(Collectors.toList());
            List<String> targetContainerFileContents = new ArrayList<>();

            for(String line : containerFileContents) {
                targetContainerFileContents.add(line);

                // LABEL declaration after an entry point or run declaration are not always supported.
                // Therefore, we add the labels directly after the base image declaration
                // Lines are never <null> at this point
                if(line.startsWith(BASE_IMAGE_ATTRIBUTE)) {
                    targetContainerFileContents.add(targetLabels);
                }
            }

            Files.write(image.getBuild().getTargetContainerFile(), targetContainerFileContents, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            String msg = "Failed to add labels (" + targetLabels + ") to Containerfile: " + e.getMessage();
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }
}
