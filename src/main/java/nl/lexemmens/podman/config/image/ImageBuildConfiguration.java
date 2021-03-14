package nl.lexemmens.podman.config.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.regex.Pattern;

public interface ImageBuildConfiguration {

    /**
     * This is the regular expression to be used to determine a multistage Containerfiles. For now we only support
     * named stages.
     */
    Pattern MULTISTAGE_CONTAINERFILE_REGEX = Pattern.compile(".*(FROM\\s.*)([ASas]\\s)([a-zA-Z].*)");

    /**
     * The default name of the Containerfile to build.
     */
    String DEFAULT_CONTAINERFILE = "Containerfile";


    /**
     * Validates this class by giving all null properties a default value.
     *
     * @param project                    The MavenProject used to derive some of the default values from.
     * @param log                        Access to Maven's log system for writing errors
     * @param failOnMissingContainerfile Whether an exception should be thrown if no Containerfile is found
     * @throws MojoExecutionException In case there is no Containerfile at the specified source location or the Containerfile is empty
     */
    void validate(MavenProject project, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException;


    boolean isValid();

    List<String> getAllTags();
}
