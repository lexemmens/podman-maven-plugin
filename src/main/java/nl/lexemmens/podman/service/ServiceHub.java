package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegateImpl;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
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
    private final BuildahExecutorService buildahExecutorService;
    private final SkopeoExecutorService skopeoExecutorService;
    private final ContainerfileDecorator containerfileDecorator;
    private final AuthenticationService authenticationService;
    private final MavenProjectHelper mavenProjectHelper;

    private final SecurityContextService securityContextService;

    /**
     * <p>
     * Constructs a new instance of this class
     * </p>
     *
     * @param log                 Access to Maven's log system
     * @param mavenProject        The MavenProject that is being built
     * @param mavenFileFilter     The {@link MavenFileFilter} service
     * @param podmanConfig        Holds global configuration for Podman
     * @param skopeoConfiguration Holds global configuration for Skopeo
     * @param mavenSettings       Access to Maven's settings file
     * @param settingsDecrypter   Access to Maven's settings decryption service
     * @param mavenProjectHelper  The MavenProjectHelper service
     */
    ServiceHub(Log log, MavenProject mavenProject, MavenFileFilter mavenFileFilter, PodmanConfiguration podmanConfig, SkopeoConfiguration skopeoConfiguration, Settings mavenSettings, SettingsDecrypter settingsDecrypter, MavenProjectHelper mavenProjectHelper) {
        this.podmanExecutorService = new PodmanExecutorService(log, podmanConfig, new CommandExecutorDelegateImpl());
        this.buildahExecutorService = new BuildahExecutorService(log, podmanConfig, new CommandExecutorDelegateImpl());
        this.skopeoExecutorService = new SkopeoExecutorService(log, skopeoConfiguration, new CommandExecutorDelegateImpl());
        this.containerfileDecorator = new ContainerfileDecorator(log, mavenFileFilter, mavenProject);
        this.authenticationService = new AuthenticationService(log, podmanExecutorService, mavenSettings, settingsDecrypter);
        this.securityContextService = new SecurityContextService(log, podmanConfig, new CommandExecutorDelegateImpl());
        this.mavenProjectHelper = mavenProjectHelper;
    }

    /**
     * Returns a reference to the FileFilterService class
     *
     * @return The {@link ContainerfileDecorator}
     */
    public ContainerfileDecorator getContainerfileDecorator() {
        return containerfileDecorator;
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

    /**
     * Returns a reference to the {@link BuildahExecutorService}
     *
     * @return The {@link BuildahExecutorService}
     */
    public BuildahExecutorService getBuildahExecutorService() {
        return buildahExecutorService;
    }

    /**
     * Returns a reference to the {@link MavenProjectHelper}
     *
     * @return The {@link MavenProjectHelper}
     */
    public MavenProjectHelper getMavenProjectHelper() {
        return mavenProjectHelper;
    }

    public SkopeoExecutorService getSkopeoExecutorService() {
        return skopeoExecutorService;
    }

    public SecurityContextService getSecurityContextService() {
        return securityContextService;
    }
}
