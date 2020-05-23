package nl.lexemmens.podman;

import nl.lexemmens.podman.service.CommandExecutorService;
import nl.lexemmens.podman.service.ServiceHub;
import nl.lexemmens.podman.service.ServiceHubFactory;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.mockito.Mock;

public abstract class AbstractMojoTest {

    @Mock
    protected Settings settings;

    @Mock
    protected CommandExecutorService commandExecutorService;

    @Mock
    protected MavenProject mavenProject;

    @Mock
    protected MavenFileFilter mavenFileFilter;

    @Mock
    protected ServiceHubFactory serviceHubFactory;

    @Mock
    protected ServiceHub serviceHub;

    @Mock
    protected SettingsDecrypter settingsDecrypter;

    @Mock
    protected Log log;

}
