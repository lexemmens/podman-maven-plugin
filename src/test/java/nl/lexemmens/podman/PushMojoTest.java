package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.image.TestImageConfigurationBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PushMojoTest extends AbstractMojoTest {

    @InjectMocks
    private PushMojo pushMojo;

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        configureMojo(true, false, null, null, null, false, false);

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipAuthenticationAndPush() throws MojoExecutionException {
        configureMojo(false, true, null, "sample", null, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Pushing container images is skipped."));
    }

    @Test
    public void testSkipPushWhenTagsNull() throws MojoExecutionException {
        configureMojo(false, false, "registry.example.com", "sample", null, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container image named sample"));
    }

    @Test
    public void testSkipPushWhenTagsEmpty() throws MojoExecutionException {
        configureMojo(false, false, "registry.example.com", "sample", new String[]{}, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container image named sample"));
    }

    @Test
    public void testPushWithoutTargetRegistryFails() throws MojoExecutionException {
        configureMojo(false, false, null, "sample", new String[]{}, true, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);

        Assertions.assertThrows(MojoExecutionException.class, pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(log, times(1)).error(Mockito.eq("Failed to push container images. No registry specified. Configure the registry by adding the <pushRegistry><!-- registry --></pushRegistry> tag to your configuration."));

    }

    @Test
    public void testPushWithoutCleaningUpLocalImage() throws MojoExecutionException {
        configureMojo(false, false, "registry.example.com", "sample", new String[]{}, true, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));

        verify(commandExecutorService, times(1)).runCommand(new File("target/podman-test"),
                true,
                false,
                "podman",
                "push",
                "",
                "registry.example.com/sample:1.0.0");
    }

    @Test
    public void testPushWithValidAuthentication() throws MojoExecutionException {
        configureMojo(false, false, "registry.example.com", "sample", new String[]{}, true, false);
        pushMojo.registries = new String[]{"registries.example.com"};
        pushMojo.skipAuth = false;

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(serviceHub.getAuthenticationService()).thenReturn(authenticationService);

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(0)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));

        verify(commandExecutorService, times(1)).runCommand(new File("target/podman-test"),
                true,
                false,
                "podman",
                "push",
                "",
                "registry.example.com/sample:1.0.0");
    }

    @Test
    public void testPushWithCleaningUpLocalImage() throws MojoExecutionException {
        configureMojo(false, false, "registry.example.com", "sample", new String[]{}, true, true);

        pushMojo.deleteLocalImageAfterPush = true;

        String imageName = "registry.example.com/sample:1.0.0";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(log, times(1)).info(Mockito.eq("Removing image " + imageName + " from the local repository"));

        File outputDir = new File("target/podman-test");
        verify(commandExecutorService, times(1)).runCommand(outputDir,
                true,
                false,
                "podman",
                "push",
                "",
                imageName);

        verify(commandExecutorService, times(1)).runCommand(outputDir,
                "podman",
                "rmi",
                imageName);
    }

    private void configureMojo(boolean skipAll, boolean skipPush, String targetRegistry, String name, String[] tags, boolean useMavenProjectVersion, boolean deleteLocalImageAfterPush) {
        ImageConfiguration image = new TestImageConfigurationBuilder(name)
                .setTags(tags)
                .setUseMavenProjectVersion(useMavenProjectVersion)
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .build();

        pushMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        pushMojo.skip = skipAll;
        pushMojo.skipAuth = true;
        pushMojo.skipPush = skipPush;
        pushMojo.pushRegistry = targetRegistry;
        pushMojo.registries = new String[]{targetRegistry};
        pushMojo.deleteLocalImageAfterPush = deleteLocalImageAfterPush;
        pushMojo.images = List.of(image);
    }

}
