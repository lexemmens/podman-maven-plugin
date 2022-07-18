package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
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
     * @param mavenProject      Reference to the current Maven Project.
     * @param mavenFileFilter   Access to Maven's file filtering service
     * @param podmanConfig      Holds global configuration for Podman
     * @param skopeoConfig      Holds global configuration for skopeo
     * @param mavenSettings     Access to the Maven Settings
     * @param settingsDecrypter Access to Maven's {@link SettingsDecrypter} service
     * @param mavenProjectHelper Access to Maven's {@link MavenProjectHelper} service
     * @return A new instance of the {@link ServiceHub}
     */
    public ServiceHub createServiceHub(Log log, MavenProject mavenProject, MavenFileFilter mavenFileFilter, PodmanConfiguration podmanConfig,
                                       SkopeoConfiguration skopeoConfig,
                                       Settings mavenSettings, SettingsDecrypter settingsDecrypter, MavenProjectHelper mavenProjectHelper) {
        return new ServiceHub(log, mavenProject, mavenFileFilter, podmanConfig, skopeoConfig, mavenSettings, settingsDecrypter, mavenProjectHelper);
    }

}
