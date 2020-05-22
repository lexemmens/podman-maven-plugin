package nl.lexemmens.podman.service;

import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Factory that allows creation of a new {@link ServiceHub} class
 */
@Component(role = ServiceHubFactory.class, hint = "default")
public class ServiceHubFactory {

    /**
     * Creates a new {@link ServiceHub} instance.
     * @param log Access to Maven's log
     * @param mavenFileFilter The MavenFileFilter instance
     * @return A new instance of the {@link ServiceHub}
     */
    public ServiceHub createServiceHub(Log log, MavenFileFilter mavenFileFilter, TlsVerify tlsVerify, Settings mavenSettings, SettingsDecrypter settingsDecrypter) {
        return new ServiceHub(log, mavenFileFilter, tlsVerify, mavenSettings, settingsDecrypter);
    }

}
