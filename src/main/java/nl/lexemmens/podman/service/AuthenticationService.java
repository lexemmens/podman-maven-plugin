package nl.lexemmens.podman.service;

import nl.lexemmens.podman.authentication.AuthConfig;
import nl.lexemmens.podman.authentication.AuthConfigFactory;
import nl.lexemmens.podman.enumeration.TlsVerify;
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
 * Service used to authenticate and verify authentication for the configured registries.
 */
public class AuthenticationService {

    private static final String PODMAN_CMD = "podman";
    private static final String LOGIN_CMD = "login";
    private static final String TLS_VERIFY_CMD = "--tls-verify=";

    private static final String AUTHS_KEY_PODMAN_CFG = "auths";

    private static final Path PODMAN_CREDENTIAL_LOCATION = Paths.get("/run/user/1000/containers/auth.json");

    private final Log log;
    private final CommandExecutorService cmdExecutorService;
    private final AuthConfigFactory authConfigFactory;

    private final TlsVerify tlsVerify;

    /**
     * Constructs a new instance of this service
     *
     * @param log Access to MAven's log system
     * @param cmdExecutorService The command executor service
     * @param mavenSetings The Maven Settings
     * @param settingsDecrypter The SettingsDecrypter service from Maven core
     * @param tlsVerify Whether TLS verification should be used.
     */
    public AuthenticationService(Log log, CommandExecutorService cmdExecutorService, Settings mavenSetings, SettingsDecrypter settingsDecrypter, TlsVerify tlsVerify) {
        this.cmdExecutorService = cmdExecutorService;
        this.log = log;
        this.tlsVerify = tlsVerify;
        this.authConfigFactory = new AuthConfigFactory(mavenSetings, settingsDecrypter);
    }

    /**
     * Ensures that credentials are available for the registries that are configured for this plugin, by executing a series of checks in a particular
     * order.
     * <p/>
     * This method assumes authentication has taken place when the configured registries are also present in Podman's authentication file.
     * <p/>
     * This method will throw {@link MojoExecutionException} if there are no registries configured whilst this method is invoked (it means that authentication
     * is not skipped).
     * <p/>
     * When there are registries configured that are not present in Podman's default authentication file, this method will attempt authentication for those registries.
     *
     * @param registries The registries to authenticate to
     * @throws MojoExecutionException In case authentication failed
     */
    public void authenticate(String[] registries) throws MojoExecutionException {
        log.info("Checking authentication status...");
        if(registries == null || registries.length == 0) {
            String msg = "No registries have been configured but authentication is not skipped. If you want to skip authentication, run again with 'podman.skip.auth' set to true";
            log.error(msg);
            throw new MojoExecutionException(msg);
        }

        if(Files.exists(PODMAN_CREDENTIAL_LOCATION)) {
            log.debug("Checking unauthenticated registries...");
            authenticateUnauthenticatedRegistries(registries);
        } else {
            log.debug("Authenticating all registries...");
            authenticateRegistries(registries);
        }

        log.info("Authentication status: OK!");
    }

    private void authenticateUnauthenticatedRegistries(String[] registries) throws MojoExecutionException {
        Set<String> authenticatedRegistries = getAuthenticatedRegistries();
        List<String> unauthenticatedRegistries = new ArrayList<>();
        for(String registry : registries) {
            if(!authenticatedRegistries.contains(registry)) {
                unauthenticatedRegistries.add(registry);
            }
        }

        authenticateRegistries(unauthenticatedRegistries.toArray(new String[]{}));
    }

    private void authenticateRegistries(String[] registries) throws MojoExecutionException {
        for(String registry : registries) {
            Optional<AuthConfig> authConfigOptional = authConfigFactory.getAuthConfigForRegistry(registry);
            if(authConfigOptional.isPresent()) {
                AuthConfig authConfig = authConfigOptional.get();
                authenticate(authConfig.getRegistry(), authConfig.getUsername(), authConfig.getPassword());
            } else {
                String msg = "Credentials are missing for registry " + registry + ". Add credentials by specifying the server in the Maven Settings.";
                log.error(msg);
                throw new MojoExecutionException(msg);
            }
        }
    }

    private void authenticate(String registry, String username, String password) throws MojoExecutionException {
        log.debug("Authenticating " + registry);
        cmdExecutorService.runCommand(new File("/"),
                false,
                true,
                PODMAN_CMD,
                LOGIN_CMD,
                tlsVerify.getCommand(),
                registry,
                "-u",
                username,
                "-p",
                password);
    }

    private Set<String> getAuthenticatedRegistries() throws MojoExecutionException {
        Set<String> authenticatedRegistries = new HashSet<>();
        JSONObject podmanConfigJson = readPodmanConfig();
        if(podmanConfigJson == null || !podmanConfigJson.has("auths")) {
            log.debug("No authenticated registries were found.");
        } else {
            Object auths = podmanConfigJson.get(AUTHS_KEY_PODMAN_CFG);
            if(auths instanceof JSONObject) {
                authenticatedRegistries = ((JSONObject) auths).keySet();
            } else {
                log.warn("Failed to read authenticated registries. Maven might re-authenticate...");
            }
        }

        return authenticatedRegistries;
    }

    private static JSONObject readPodmanConfig() throws MojoExecutionException {
        Reader reader = getFileReader();
        return reader != null ? new JSONObject(new JSONTokener(reader)) : null;
    }

    private static Reader getFileReader() throws MojoExecutionException{
        File file = PODMAN_CREDENTIAL_LOCATION.toFile();
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
