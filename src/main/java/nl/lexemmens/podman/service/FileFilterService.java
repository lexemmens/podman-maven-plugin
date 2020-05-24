package nl.lexemmens.podman.service;

import nl.lexemmens.podman.context.BuildContext;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;

/**
 * <p>
 * FilterSupport class that brings support for accessing properties from a {@link MavenProject}. Both properties
 * specified via the properties tag in a pom file as well as the default Maven properties are supported
 * </p>
 */
public class FileFilterService {

    /**
     * Logger instance
     */
    private final Log log;

    /**
     * The MavenPropertyReader that can be used to get access to default Maven properties.
     */
    private final MavenFileFilter mavenFileFilter;

    /**
     * Constructs a new instance of this FilterSupport class.
     *
     * @param log             The logger
     * @param mavenFileFilter The MavenProject
     */
    public FileFilterService(Log log, MavenFileFilter mavenFileFilter) {
        this.log = log;
        this.mavenFileFilter = mavenFileFilter;
    }

    /**
     * <p>
     * Filters a Dockerfile by using the {@link MavenFileFilter} service to filter the file.
     * </p>
     *
     * @param ctx The BuildContext that contains the source and target Dockerfile paths
     * @throws MojoExecutionException When the Dockerfile cannot be filtered.
     */
    public void filterDockerfile(BuildContext ctx) throws MojoExecutionException {
        log.debug("Filtering Dockerfile. Source: " + ctx.getSourceDockerfile() + ", target: " + ctx.getTargetDockerfile());
        try {
            MavenFileFilterRequest fileFilterRequest = new MavenFileFilterRequest();
            fileFilterRequest.setEncoding("UTF8");
            fileFilterRequest.setFiltering(true);
            fileFilterRequest.setFrom(ctx.getSourceDockerfile().toFile());
            fileFilterRequest.setTo(ctx.getTargetDockerfile().toFile());
            fileFilterRequest.setMavenProject(ctx.getMavenProject());

            mavenFileFilter.copyFile(fileFilterRequest);
        } catch (MavenFilteringException e) {
            String msg = "Failed to filter Dockerfile! " + e.getMessage();
            log.error(msg, e);
            throw new MojoExecutionException(msg, e);
        }

    }
}
