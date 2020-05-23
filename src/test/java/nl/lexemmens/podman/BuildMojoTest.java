package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.service.ServiceHub;
import nl.lexemmens.podman.service.ServiceHubFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BuildMojoTest {

    @Mock
    private MavenFileFilter mavenFileFilter;

    @Mock
    private ServiceHubFactory serviceHubFactory;

    @Mock
    private SettingsDecrypter settingsDecrypter;

    @Mock
    private Settings mavenSettings;

    @Mock
    private Log log;

    @InjectMocks
    private BuildMojo buildMojo;

    @Before
    public void setup() {

    }

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        buildMojo.skip = true;
        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipAuthenticationAndBuild() throws MojoExecutionException {
        buildMojo.skipAuth = true;
        buildMojo.skipBuild = true;
        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Building container images is skipped."));
    }

//    @Test
//    public void testBuildWithoutTag() throws MojoExecutionException {
//        buildMojo.skipTag = true;
//        buildMojo.execute();
//
//        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
//    }

}
