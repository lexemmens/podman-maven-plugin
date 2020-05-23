package nl.lexemmens.podman.service;

import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;

/**
 * Context class providing access to runtime requirements, such as support classes, image hash
 */
public class ServiceHub {

    private final CommandExecutorService cmdExecutor;
    private final FileFilterService fileFilterService;
    private final AuthenticationService authenticationService;

    /**
     * Constructs a new instance of this class
     *
     * @param log             The log from Maven
     * @param mavenFileFilter The {@link MavenFileFilter} service
     */
    ServiceHub(Log log, MavenFileFilter mavenFileFilter, TlsVerify tlsVerify, Settings mavenSettings, SettingsDecrypter settingsDecrypter) {
        this.cmdExecutor = new CommandExecutorService(log);
        this.fileFilterService = new FileFilterService(log, mavenFileFilter);
        this.authenticationService = new AuthenticationService(log, cmdExecutor, mavenSettings, settingsDecrypter, tlsVerify);
    }

    /**
     * Returns a reference to the CommandExecutorService
     */
    public CommandExecutorService getCommandExecutorService() {
        return cmdExecutor;
    }

    /**
     * Returns a reference to the FileFilterService class
     */
    public FileFilterService getFileFilterService() {
        return fileFilterService;
    }

    /**
     * Returns a reference to the {@link AuthenticationService}
     */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }
}
