package nl.lexemmens.podman.config.image;

import nl.lexemmens.podman.config.image.ImageBuildConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public interface ImageConfiguration {

    void initAndValidate(MavenProject mavenProject, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException;

    ImageBuildConfiguration getBuild();
}
