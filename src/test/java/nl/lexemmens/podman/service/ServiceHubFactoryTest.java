package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class ServiceHubFactoryTest {

    @Mock
    private Log log;

    @Mock
    private MavenFileFilter mavenFileFilter;

    @Mock
    private Settings mavenSettings;

    @Mock
    private SettingsDecrypter settingsDecrypter;

    @Mock
    private MavenProject mavenProject;

    @Mock
    private MavenProjectHelper mavenProjectHelper;

    @Mock
    private PodmanConfiguration podmanConfiguration;

    @Mock
    private SkopeoConfiguration skopeoConfiguration;

    @Test
    public void testServiceHubFactory() {
        ServiceHubFactory serviceHubFactory = new ServiceHubFactory();
        ServiceHub serviceHub = serviceHubFactory.createServiceHub(log, mavenProject, mavenFileFilter, podmanConfiguration, skopeoConfiguration, mavenSettings, settingsDecrypter, mavenProjectHelper);

        Assertions.assertNotNull(serviceHub.getPodmanExecutorService());
        Assertions.assertNotNull(serviceHub.getContainerfileDecorator());
        Assertions.assertNotNull(serviceHub.getAuthenticationService());
    }

}
