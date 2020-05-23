package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.service.ServiceHubFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PushMojoTest extends AbstractMojoTest {

    @InjectMocks
    private PushMojo pushMojo;

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

    @Test
    public void testPushWithoutCleaningUpLocalImage() throws MojoExecutionException {
        pushMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        pushMojo.skipAuth = true;
        pushMojo.useMavenProjectVersion = true;
        pushMojo.tagVersion = null;
        pushMojo.tags = new String[]{"sampleTag"};
        pushMojo.targetRegistry = "registry.example.com";
        pushMojo.outputDirectory = new File("./target/podman-test");

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
        pushMojo.deleteLocalImageAfterPush = true;
        pushMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        pushMojo.skipAuth = true;
        pushMojo.useMavenProjectVersion = true;
        pushMojo.tagVersion = null;
        pushMojo.tags = new String[]{"sampleTag"};
        pushMojo.targetRegistry = "registry.example.com";
        pushMojo.outputDirectory = new File("./target/podman-test");

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


}
