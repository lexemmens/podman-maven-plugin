package nl.lexemmens.podman.service;

import nl.lexemmens.podman.enumeration.TlsVerify;
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

    private final CommandExecutorService cmdExecutor;
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
     * @param tlsVerify         Whether TLS Verification should be used
     * @param mavenSettings     Access to Maven's settings file
     * @param settingsDecrypter Access to Maven's settings decryption service
     */
    ServiceHub(Log log, MavenProject mavenProject, MavenFileFilter mavenFileFilter, TlsVerify tlsVerify, Settings mavenSettings, SettingsDecrypter settingsDecrypter) {
        this.cmdExecutor = new CommandExecutorService(log);
        this.dockerfileDecorator = new DockerfileDecorator(log, mavenFileFilter, mavenProject);
        this.authenticationService = new AuthenticationService(log, cmdExecutor, mavenSettings, settingsDecrypter, tlsVerify);
    }

    /**
     * Returns a reference to the CommandExecutorService
     *
     * @return The {@link CommandExecutorService}
     */
    public CommandExecutorService getCommandExecutorService() {
        return cmdExecutor;
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
}
