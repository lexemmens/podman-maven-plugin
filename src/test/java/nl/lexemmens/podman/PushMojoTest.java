package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PushMojoTest extends AbstractMojoTest {

    @InjectMocks
    private PushMojo pushMojo;

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        configureMojo(true, false,  null,  null, false,false);

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipAuthenticationAndPush() throws MojoExecutionException {
        configureMojo(false, true,  null,  null, false, false);

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Pushing container images is skipped."));
    }

    @Test
    public void testSkipPushWhenTagsNull() throws MojoExecutionException {
        configureMojo(false, false,  null,  null, false, false);

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container images."));
    }

    @Test
    public void testSkipPushWhenTagsEmpty() throws MojoExecutionException {
        configureMojo(false, false,  null,  new String[]{}, false, false);

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container images."));
    }

    @Test
    public void testPushWithoutCleaningUpLocalImage() throws MojoExecutionException {
        configureMojo(false, false,  "registry.example.com",  new String[]{"sampleTag"}, true, false);

        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));

        verify(commandExecutorService, times(1)).runCommand(pushMojo.outputDirectory,
                true,
                false,
                "podman",
                "push",
                "",
                "registry.example.com/sampleTag:1.0.0");
    }

    @Test
    public void testPushWithCleaningUpLocalImage() throws MojoExecutionException {
        configureMojo(false, false,  "registry.example.com",  new String[]{"sampleTag"}, true, true);

        pushMojo.deleteLocalImageAfterPush = true;

        String imageName = "registry.example.com/sampleTag:1.0.0";

        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(log, times(1)).info(Mockito.eq("Removing image " + imageName + " from the local repository"));

        verify(commandExecutorService, times(1)).runCommand(pushMojo.outputDirectory,
                true,
                false,
                "podman",
                "push",
                "",
                imageName);

        verify(commandExecutorService, times(1)).runCommand(pushMojo.outputDirectory,
                "podman",
                "rmi",
                imageName);
    }

    private void configureMojo(boolean skipAll, boolean skipPush, String targetRegistry, String[] tags, boolean useMavenProjectVersion, boolean deleteLocalImageAfterPush) {
        pushMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        pushMojo.skip = skipAll;
        pushMojo.skipAuth = true;
        pushMojo.skipPush = skipPush;
        pushMojo.outputDirectory = new File("target/podman-test");
        pushMojo.targetRegistry = targetRegistry;
        pushMojo.useMavenProjectVersion = useMavenProjectVersion;
        pushMojo.tags = tags;
        pushMojo.targetRegistry = "registry.example.com";
        pushMojo.deleteLocalImageAfterPush = deleteLocalImageAfterPush;
    }

}
