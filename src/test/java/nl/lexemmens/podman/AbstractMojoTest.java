package nl.lexemmens.podman;

import nl.lexemmens.podman.service.*;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.mockito.Mock;

import java.io.File;

public abstract class AbstractMojoTest {

    protected static final String DEFAULT_DOCKERFILE_DIR = "src/test/resources";
    protected static final File DEFAULT_TEST_OUTPUT_DIR = new File("target");

    @Mock
    protected Settings settings;

    @Mock
    protected PodmanExecutorService podmanExecutorService;

    @Mock
    protected BuildahExecutorService buildahExecutorService;

    @Mock
    protected AuthenticationService authenticationService;

    @Mock
    protected MavenProject mavenProject;

    @Mock
    protected Build build;

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
