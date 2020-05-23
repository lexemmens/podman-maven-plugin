package nl.lexemmens.podman;

import nl.lexemmens.podman.context.BuildContext;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BuildMojoTest extends AbstractMojoTest {

    @InjectMocks
    private BuildMojo buildMojo;

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

    @Test
    public void testBuildWithoutTag() throws MojoExecutionException {
        buildMojo.skipTag = true;
        buildMojo.skipAuth = true;
        buildMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        buildMojo.dockerFileDir = new File("src/test/resources");
        buildMojo.outputDirectory = new File("target/podman-test");
        buildMojo.tagVersion = "1.0.0";

        List<String> processOutput = List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76");

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getFileFilterService()).thenReturn(fileFilterService);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(commandExecutorService.runCommand(
                eq(buildMojo.outputDirectory),
                eq(true),
                eq(false),
                eq("podman"),
                eq("build"),
                eq(""),
                eq(".")))
                .thenReturn(processOutput);

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container images is skipped."));
        verify(fileFilterService, Mockito.times(1)).filterDockerfile(isA(BuildContext.class));

        verify(commandExecutorService, times(1)).runCommand(buildMojo.outputDirectory,
                true,
                false,
                "podman",
                "build",
                "",
                ".");
    }

    @Test
    public void testBuildAndTaggingWithNoTags() throws MojoExecutionException {
        buildMojo.skipTag = false;
        buildMojo.skipAuth = true;
        buildMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        buildMojo.dockerFileDir = new File("src/test/resources");
        buildMojo.outputDirectory = new File("target/podman-test");
        buildMojo.tagVersion = "1.0.0";

        List<String> processOutput = List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76");

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getFileFilterService()).thenReturn(fileFilterService);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(commandExecutorService.runCommand(
                eq(buildMojo.outputDirectory),
                eq(true),
                eq(false),
                eq("podman"),
                eq("build"),
                eq(""),
                eq(".")))
                .thenReturn(processOutput);

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(fileFilterService, Mockito.times(1)).filterDockerfile(isA(BuildContext.class));

        verify(commandExecutorService, times(1)).runCommand(buildMojo.outputDirectory,
                true,
                false,
                "podman",
                "build",
                "",
                ".");
    }

    @Test
    public void testBuildWithTag() throws MojoExecutionException {
        buildMojo.skipTag = false;
        buildMojo.skipAuth = true;
        buildMojo.tlsVerify = TlsVerify.FALSE;
        buildMojo.dockerFileDir = new File("src/test/resources");
        buildMojo.outputDirectory = new File("target/podman-test");
        buildMojo.tagVersion = "1.0.0";
        buildMojo.tags = new String[]{"sampleTag"};
        buildMojo.targetRegistry = "registry.example.com";

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        List<String> processOutput = List.of(imageHash);

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getFileFilterService()).thenReturn(fileFilterService);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(commandExecutorService.runCommand(
                eq(buildMojo.outputDirectory),
                eq(true),
                eq(false),
                eq("podman"),
                eq("build"),
                eq("--tls-verify=false"),
                eq(".")))
                .thenReturn(processOutput);

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(0)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container image " + imageHash + " as registry.example.com/sampleTag:1.0.0"));
        verify(fileFilterService, Mockito.times(1)).filterDockerfile(isA(BuildContext.class));

        verify(commandExecutorService, times(1)).runCommand(buildMojo.outputDirectory,
                true,
                false,
                "podman",
                "build",
                "--tls-verify=false",
                ".");
    }

    @Test
    public void testBuildContextNoDockerfile() throws MojoExecutionException {
        buildMojo.tlsVerify = TlsVerify.FALSE;
        buildMojo.skipAuth = true;
        buildMojo.tags = new String[]{};
        // There is no Dockerfile here
        buildMojo.dockerFileDir = new File("src/test");
        buildMojo.outputDirectory = new File("target/podman-test");
        buildMojo.tagVersion = "1.0.0";

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildContextWithEmptyDockerfile() throws MojoExecutionException {
        buildMojo.tlsVerify = TlsVerify.FALSE;
        buildMojo.skipAuth = true;
        buildMojo.tags = new String[]{};
        // There is no Dockerfile here
        buildMojo.dockerFileDir = new File("src/test/resources/emptydockerfile");
        buildMojo.outputDirectory = new File("target/podman-test");
        buildMojo.tagVersion = "1.0.0";

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }
}
