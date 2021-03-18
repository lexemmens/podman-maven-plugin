package nl.lexemmens.podman.service;

import nl.lexemmens.podman.authentication.AuthConfig;
import nl.lexemmens.podman.authentication.AuthConfigFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * <p>
 * Service used to authenticate to configured registries and verify authentication for the configured registries.
 * </p>
 * <p>
 * Note that this service does not check if the credentials are (still) valid for authenticated registries.
 * It only validates if credentials are present for registries and uses them to authenticate registries that are not yet
 * in <em>Podman</em>'s authentication file.
 * </p>
 */
public class AuthenticationService {

    private static final String AUTHS_KEY_PODMAN_CFG = "auths";

    /**
     * Environment variable pointing to the XDG_RUNTIME_DIR, where the authentication credentials are stored by default
     */
    private static final String XDG_RUNTIME_DIR = "XDG_RUNTIME_DIR";

    /**
     * Environment variable that allows overriding the default credential store location
     */
    private static final String REGISTRY_AUTH_FILE = "REGISTRY_AUTH_FILE";

    /**
     * Default file under the REGISTRY_AUTH_FILE folder containing the user credentials
     */
    private static final String AUTH_JSON_SUB_PATH = "containers/auth.json";

    /**
     * Default Docker credential file
     */
    private static final String DOCKER_CONFIG_FILE = ".docker/config.json";

    private final Log log;
    private final PodmanExecutorService podmanExecutorService;
    private final AuthConfigFactory authConfigFactory;

    /**
     * Constructs a new instance of this service
     *
     * @param log                   Provides access to Maven's log system
     * @param podmanExecutorService Service for executing commands with Podman
     * @param mavenSetings          Provides access to the Maven Settings
     * @param settingsDecrypter     Provides access to Maven's SettingsDecrypter service from Maven core
     */
    public AuthenticationService(Log log, PodmanExecutorService podmanExecutorService, Settings mavenSetings, SettingsDecrypter settingsDecrypter) {
        this.podmanExecutorService = podmanExecutorService;
        this.log = log;
        this.authConfigFactory = new AuthConfigFactory(mavenSetings, settingsDecrypter);
    }

    /**
     * <p>
     * Ensures that credentials are available for the registries that are configured for this plugin, by executing a series of checks in a particular
     * order.
     * </p>
     * <p>
     * This method assumes authentication has taken place when the configured registries are also present in Podman's authentication file.
     * </p>
     * <p>
     * When there are registries configured that are not present in Podman's default authentication file, this method will attempt authentication for those registries.
     * </p>
     * <p>
     * This method will throw a MojoExecutionException in case:
     * </p>
     * <ul>
     *     <li>No registries are passed (it means authentication is not skipped)</li>
     *     <li>Authentication fails</li>
     *     <li>Credentials for a certain registry could not be found in the Maven settings.</li>
     * </ul>
     *
     * @param registries The registries to authenticate to
     * @throws MojoExecutionException In case authentication fails, no registries were passed or credentials are missing.
     */
    public void authenticate(String[] registries) throws MojoExecutionException {
        log.info("Checking authentication status...");
        if (registries == null || registries.length == 0) {
            String msg = "No registries have been configured but authentication is not skipped. If you want to skip authentication, run again with 'podman.skip.auth' set to true";
            log.error(msg);
            throw new MojoExecutionException(msg);
        }

        List<Path> registryAuthFiles = getRegistryAuthFiles();
        if (registryAuthFiles.isEmpty()) {
            log.info("Authentication file not (yet) present. Authenticating...");
            authenticateRegistries(registries);
        } else {
            log.debug("Checking unauthenticated registries...");
            authenticateUnauthenticatedRegistries(registries, registryAuthFiles);
        }

        log.debug("Authentication status: OK!");
    }

    /**
     * <p>
     * Returns a {@link Optional} instance potentially referencing the registry authentication file. This method will first
     * try the default authentication file, located in /run/user/1000/containers/auth.json. If that file
     * is not present it will look for an environment variable named REGISTRY_AUTH_FILE, that may possibly
     * contain an alternative authentication file.
     * </p>
     * <p>
     * This method returns an {@link Optional#empty()} when no authentication file could be found
     * </p>
     *
     * @return An Optional potentially holding the location of an authentication file.
     */
    private List<Path> getRegistryAuthFiles() {
        List<Path> registryAuthFiles = new ArrayList<>();
        if (System.getenv().containsKey(REGISTRY_AUTH_FILE)) {
            Path customRegistryAuthFile = Paths.get(System.getenv(REGISTRY_AUTH_FILE));
            if (Files.exists(customRegistryAuthFile)) {
                registryAuthFiles.add(customRegistryAuthFile);
            }
        }

        if (System.getenv().containsKey(XDG_RUNTIME_DIR)) {
            Path xdgRuntimeDir = Paths.get(System.getenv(XDG_RUNTIME_DIR));
            Path defaultAuthFile = xdgRuntimeDir.resolve(AUTH_JSON_SUB_PATH);
            if (Files.exists(defaultAuthFile)) {
                registryAuthFiles.add(defaultAuthFile);
            }
        }

        // Check docker auth file
        Path dockerConfigFile = Paths.get(System.getProperty("user.home")).resolve(DOCKER_CONFIG_FILE);
        if (Files.exists(dockerConfigFile)) {
            registryAuthFiles.add(dockerConfigFile);
        }

        if (registryAuthFiles.isEmpty()) {
            log.warn("Could not locate suitable credentials for Podman. If this error persists, try running with <skipAuth>true</skipAuth>.");
        }

        return registryAuthFiles;
    }

    private void authenticateUnauthenticatedRegistries(String[] registries, List<Path> registryAuthFilePaths) throws MojoExecutionException {
        Set<String> authenticatedRegistries = getAuthenticatedRegistries(registryAuthFilePaths);
        List<String> unauthenticatedRegistries = new ArrayList<>();
        for (String registry : registries) {
            if (!authenticatedRegistries.contains(registry)) {
                unauthenticatedRegistries.add(registry);
            }
        }

        authenticateRegistries(unauthenticatedRegistries.toArray(new String[]{}));
    }

    private void authenticateRegistries(String[] registries) throws MojoExecutionException {
        for (String registry : registries) {
            Optional<AuthConfig> authConfigOptional = authConfigFactory.getAuthConfigForRegistry(registry);
            if (authConfigOptional.isPresent()) {
                AuthConfig authConfig = authConfigOptional.get();
                authenticate(authConfig.getRegistry(), authConfig.getUsername(), authConfig.getPassword());
            } else {
                String msg = "Credentials are missing for registry " + registry + ". Add credentials by specifying the server in the " +
                        "Maven's settings.xml (usually located in ~/.m2/)";
                log.error(msg);
                throw new MojoExecutionException(msg);
            }
        }
    }

    private void authenticate(String registry, String username, String password) throws MojoExecutionException {
        log.debug("Authenticating " + registry);
        podmanExecutorService.login(registry, username, password);
    }

    private Set<String> getAuthenticatedRegistries(List<Path> registryAuthFilePaths) throws MojoExecutionException {
        Set<String> authenticatedRegistries = new HashSet<>();
        for(Path registryAuthFilePath : registryAuthFilePaths) {
            JSONObject podmanConfigJson = readPodmanConfig(registryAuthFilePath);
            if (podmanConfigJson == null || !podmanConfigJson.has(AUTHS_KEY_PODMAN_CFG)) {
                log.debug("No authenticated registries were found.");
            } else {
                Object auths = podmanConfigJson.get(AUTHS_KEY_PODMAN_CFG);
                if (auths instanceof JSONObject) {
                    authenticatedRegistries.addAll(((JSONObject) auths).keySet());
                } else {
                    log.warn("Failed to read authenticated registries. Maven might re-authenticate...");
                }
            }
        }

        return authenticatedRegistries;
    }

    private static JSONObject readPodmanConfig(Path registryAuthFilePath) throws MojoExecutionException {
        Reader reader = getFileReader(registryAuthFilePath);
        return reader != null ? new JSONObject(new JSONTokener(reader)) : null;
    }

    private static Reader getFileReader(Path registryAuthFilePath) throws MojoExecutionException {
        File file = registryAuthFilePath.toFile();
        if (file.exists() && file.length() != 0) {
            try {
                return new FileReader(file);
            } catch (FileNotFoundException e) {
                // Very unlikely to happen...
                throw new MojoExecutionException("Cannot find " + file, e);
            }
        } else {
            return null;
        }
    }
}
