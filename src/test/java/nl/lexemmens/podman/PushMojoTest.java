package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHubFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
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

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PushMojoTest {

    @Mock
    private MavenFileFilter mavenFileFilter;

    @Mock
    private ServiceHubFactory serviceHubFactory;

    @Mock
    private SettingsDecrypter settingsDecrypter;

    @Mock
    private Log log;

    @InjectMocks
    private PushMojo pushMojo;

    @Before
    public void setup() {

    }

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        pushMojo.skip = true;
        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipAuthenticationAndPush() throws MojoExecutionException {
        pushMojo.skipAuth = true;
        pushMojo.skipPush = true;
        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Pushing container images is skipped."));
    }

    @Test
    public void testSkipPushWhenTagsNull() throws MojoExecutionException {
        pushMojo.skipAuth = true;
        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container images."));
    }

    @Test
    public void testSkipPushWhenTagsEmpty() throws MojoExecutionException {
        pushMojo.skipAuth = true;
        pushMojo.tags = new String[]{};
        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container images."));
    }


}
