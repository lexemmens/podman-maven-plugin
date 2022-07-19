package nl.lexemmens.podman.authentication;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

import java.util.Optional;

/**
 * Factory class to build an {@link AuthConfig} instance for a specific registry. Authentication information
 * is retrieved from the Maven settings.
 */
public class AuthConfigFactory {

    private final Settings mavenSettings;
    private final SettingsDecryptionResult decryptedSettings;

    /**
     * Constructs a new instance of this factory.
     * @param mavenSettings The Maven Settings
     * @param settingsDecrypter The SettingsDecrypter in case authentication data is encrypted
     */
    public AuthConfigFactory(Settings mavenSettings, SettingsDecrypter settingsDecrypter) {
        this.mavenSettings = mavenSettings;

        DefaultSettingsDecryptionRequest defaultSettingsDecryptionRequest = new DefaultSettingsDecryptionRequest(mavenSettings);
        decryptedSettings = settingsDecrypter.decrypt(defaultSettingsDecryptionRequest);
    }

    /**
     * Returns an {@link Optional} that may hold an instance of an {@link AuthConfig} class holding authentication information
     * for the provided registry.
     *
     * Returns an {@link Optional#empty()} if no credentials could be found in the Maven settings
     * @param registry The registry
     * @return An Optional that may hold authentication data for the provided registry
     */
    public Optional<AuthConfig> getAuthConfigForRegistry(String registry) {
        Optional<AuthConfig> authConfigOptional;
        Server server = mavenSettings.getServer(registry);
        if(server == null) {
            authConfigOptional = Optional.empty();
        } else {
            authConfigOptional = Optional.ofNullable(lookupAuthConfigForRegistry(registry));
        }

        return authConfigOptional;
    }

    private AuthConfig lookupAuthConfigForRegistry(String registry) {
        for(Server server : decryptedSettings.getServers()) {
            if(registry.equals(server.getId())) {
                return new AuthConfig(registry, server.getUsername(), server.getPassword());
            }
        }

        // This should never happen
        return null;
    }

}
