package nl.lexemmens.podman.service;

import nl.lexemmens.podman.executor.CommandExecutorDelegateImpl;
import nl.lexemmens.podman.image.PodmanConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;

/**
 * <p>
 * Context class providing access to runtime requirements, such as support classes, image hash
 * </p>
 */
public class ServiceHub {

    private final PodmanExecutorService podmanExecutorService;
    private final DockerfileDecorator dockerfileDecorator;
    private final AuthenticationService authenticationService;

    /**
     * <p>
     * Constructs a new instance of this class
     * </p>
     *
     * @param log               Access to Maven's log system
     * @param mavenProject      The MavenProject that is being built
     * @param mavenFileFilter   The {@link MavenFileFilter} service
     * @param podmanConfig      Holds global configuration for Podman
     * @param mavenSettings     Access to Maven's settings file
     * @param settingsDecrypter Access to Maven's settings decryption service
     */
    ServiceHub(Log log, MavenProject mavenProject, MavenFileFilter mavenFileFilter, PodmanConfiguration podmanConfig, Settings mavenSettings, SettingsDecrypter settingsDecrypter) {
        this.podmanExecutorService = new PodmanExecutorService(log, podmanConfig, new CommandExecutorDelegateImpl());
        this.dockerfileDecorator = new DockerfileDecorator(log, mavenFileFilter, mavenProject);
        this.authenticationService = new AuthenticationService(log, podmanExecutorService, mavenSettings, settingsDecrypter);
    }

    /**
     * Returns a reference to the FileFilterService class
     *
     * @return The {@link DockerfileDecorator}
     */
    public DockerfileDecorator getDockerfileDecorator() {
        return dockerfileDecorator;
    }

    /**
     * Returns a reference to the {@link AuthenticationService}
     *
     * @return The {@link AuthenticationService}
     */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    /**
     * Returns a reference to the {@link PodmanExecutorService}
     *
     * @return The {@link PodmanExecutorService}
     */
    public PodmanExecutorService getPodmanExecutorService() {
        return podmanExecutorService;
    }
}
