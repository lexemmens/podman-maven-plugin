package nl.lexemmens.podman.context;

import nl.lexemmens.podman.support.CommandExecutor;
import nl.lexemmens.podman.support.FilterSupport;
import nl.lexemmens.podman.support.IOSupport;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.Optional;

/**
 * Context class providing access to runtime requirements, such as support classes, image hash
 */
public final class PodmanContext {

    private final CommandExecutor cmdExecutor;
    private final FilterSupport filterSupport;
    private final IOSupport ioSupport;
    private String imageHash = null;

    /**
     * Constructs a new instance of this class
     * @param log The log from Maven
     * @param project The Maven project
     */
    public PodmanContext(Log log, MavenProject project) {
        cmdExecutor = new CommandExecutor(log);
        ioSupport = new IOSupport(log);
        filterSupport = new FilterSupport(log, project, ioSupport);
    }

    /**
     * Returns a reference to the CommandExecutor
     */
    public final CommandExecutor getCmdExecutor() {
        return cmdExecutor;
    }

    /**
     * Returns a reference to the FilterSupport class
     */
    public final FilterSupport getFilterSupport() {
        return filterSupport;
    }

    /**
     * Returns an Optional of the image hash
     */
    public final Optional<String> getImageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * Sets the image hash to a specific value
     */
    public final  void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    /**
     * Returns a reference to the IOSupport class
     */
    public final IOSupport getIoSupport() {
        return ioSupport;
    }
}
