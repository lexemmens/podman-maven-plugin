package nl.lexemmens.podman.service;

import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.component.annotations.Component;

/**
 * <p>
 * Factory that allows creation of a new {@link ServiceHub} class
 * </p>
 */
@Component(role = ServiceHubFactory.class, hint = "default")
public class ServiceHubFactory {

    /**
     * <p>
     * Creates a new {@link ServiceHub} instance.
     * </p>
     *
     * @param log               Access to Maven's log system
     * @param mavenFileFilter   Access to Maven's file filtering service
     * @param tlsVerify         Whether TLS verification should be used by any of the services provided by this hub
     * @param mavenSettings     Access to the Maven Settings
     * @param settingsDecrypter Access to Maven's {@link SettingsDecrypter} service
     * @return A new instance of the {@link ServiceHub}
     */
    public ServiceHub createServiceHub(Log log, MavenProject mavenProject, MavenFileFilter mavenFileFilter, TlsVerify tlsVerify, Settings mavenSettings, SettingsDecrypter settingsDecrypter) {
        return new ServiceHub(log, mavenProject, mavenFileFilter, tlsVerify, mavenSettings, settingsDecrypter);
    }

}
