package nl.lexemmens.podman.support;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FilterSupport class that brings support for accessing properties from a {@link MavenProject}. Both properties
 * specified via the properties tag in a pom file as well as the default Maven properties are supported
 */
public class FilterSupport {

    /**
     * Regular expression that matches the default property style
     */
    private static final String PROPERTY_REGEX = "\\$\\{[A-Za-z0-9.].*}";

    /**
     * Logger instance
     */
    private final Log log;

    /**
     * The MavenPropertyReader that can be used to get access to default Maven properties.
     */
    private final MavenPropertyReader propertyReader;

    private final IOSupport ioSupport;

    /**
     * Constructs a new instance of this FilterSupport class.
     *
     * @param log     The logger
     * @param project The MavenProject
     */
    public FilterSupport(Log log, MavenProject project, IOSupport ioSupport) {
        this.log = log;
        this.propertyReader = new MavenPropertyReader(project);
        this.ioSupport = ioSupport;
    }

    /**
     * Filters a Dockerfile by replacing all properties by actual values. Supports both values listed in the properties tag in <em>a</em> pom file
     * and the default Maven properties.
     * <p/>
     * Example:
     * <ul>
     *     <li>${project.artifactId} will be converted to the actual artifactId</li>
     *     <li>${some.property.name} will be converted to the value corresponding to that value</li>
     * </ul>
     * <p/>
     * Access to environment variables is not supported.
     *
     * @param dockerFile           The raw Dockerfile containing the property keys
     * @param targetDockerfilePath The target location of the filtered Dockerfile
     * @param projectProperties    All the properties from the Maven project
     */
    public void filterDockerfile(Path dockerFile, Path targetDockerfilePath, Properties projectProperties) throws MojoExecutionException {
        List<String> filteredDockerfileContents = getFilteredDockerfileContents(dockerFile, projectProperties);
        log.debug("Using target Dockerfile: " + targetDockerfilePath);

        Path targetDockerfile = ioSupport.createFile(targetDockerfilePath);
        if (Files.isWritable(targetDockerfile)) {
            ioSupport.writeContentsToFile(targetDockerfile, filteredDockerfileContents);
        } else {
            log.error("Could not open temporary Dockerfile for writing...");
        }
    }

    private List<String> getFilteredDockerfileContents(Path dockerfile, Properties projectProperties) throws MojoExecutionException {
        log.debug("Filtering Dockerfile contents...");

        List<String> dockerFileContents = ioSupport.readFileContents(dockerfile);
        List<String> targetDockerFileContents = new ArrayList<>();

        Pattern propertyPattern = Pattern.compile(PROPERTY_REGEX);
        for (String line : dockerFileContents) {
            log.debug("Processing line " + line);
            Matcher matcher = propertyPattern.matcher(line);
            if (matcher.find()) {
                matcher.reset();
                while (matcher.find()) {
                    String match = matcher.group();
                    Object propertyValue = projectProperties.get(match.substring(2, match.length() - 1));

                    if (propertyValue == null) {
                        propertyValue = propertyReader.getProperty(match.substring(2, match.length() - 1));
                    }

                    log.debug("Replacing '" + match + "' with '" + propertyValue + "'.");
                    targetDockerFileContents.add(line.replaceAll(PROPERTY_REGEX, propertyValue.toString()));
                }
            } else {
                log.debug("Line has no properties. Skipping.");
                targetDockerFileContents.add(line);
            }
        }

        return targetDockerFileContents;
    }


}
