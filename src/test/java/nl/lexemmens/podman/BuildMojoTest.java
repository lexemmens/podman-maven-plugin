package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.service.FileFilterService;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.junit.Before;
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
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BuildMojoTest extends AbstractMojoTest {

    public static final String DEFAULT_DOCKERFILE_DIR = "src/test/resources";
    @InjectMocks
    private BuildMojo buildMojo;

    private FileFilterService fileFilterService;

    @Before
    public void setup() {
        initMocks(this);

        fileFilterService = new FileFilterService(log, mavenFileFilter);
    }

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        configureMojo(true, false, false, null, null, null);

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipAuthenticationAndBuild() throws MojoExecutionException {
        configureMojo(false, true, false, null, null, null);

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Building container images is skipped."));
    }

    @Test
    public void testBuildWithoutTag() throws MojoExecutionException, MavenFilteringException {
        configureMojo(false, false, true, DEFAULT_DOCKERFILE_DIR, "1.0.0", null);

        List<String> processOutput = List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76");

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getFileFilterService()).thenReturn(fileFilterService);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(commandExecutorService.runCommand(
                isA(File.class),
                isA(Boolean.class),
                isA(Boolean.class),
                isA(String.class),
                isA(String.class),
                isA(String.class),
                isA(String.class)))
                .thenReturn(processOutput);

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container images is skipped."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(commandExecutorService, times(1)).runCommand(buildMojo.outputDirectory,
                true,
                false,
                "podman",
                "build",
                "--tls-verify=false",
                ".");
    }

    @Test
    public void testBuildAndTaggingWithNullTags() throws MojoExecutionException, MavenFilteringException {
        configureMojo(false, false, false, DEFAULT_DOCKERFILE_DIR, "1.0.0", null);

        List<String> processOutput = List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76");

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getFileFilterService()).thenReturn(fileFilterService);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(commandExecutorService.runCommand(
                isA(File.class),
                isA(Boolean.class),
                isA(Boolean.class),
                isA(String.class),
                isA(String.class),
                isA(String.class),
                isA(String.class)))
                .thenReturn(processOutput);

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(commandExecutorService, times(1)).runCommand(buildMojo.outputDirectory,
                true,
                false,
                "podman",
                "build",
                "--tls-verify=false",
                ".");
    }

    @Test
    public void testBuildAndTaggingWithEmptyTags() throws MojoExecutionException, MavenFilteringException {
        configureMojo(false, false, false, DEFAULT_DOCKERFILE_DIR, "1.0.0", new String[]{});

        List<String> processOutput = List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76");

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getFileFilterService()).thenReturn(fileFilterService);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(commandExecutorService.runCommand(
                isA(File.class),
                isA(Boolean.class),
                isA(Boolean.class),
                isA(String.class),
                isA(String.class),
                isA(String.class),
                isA(String.class)))
                .thenReturn(processOutput);

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(commandExecutorService, times(1)).runCommand(buildMojo.outputDirectory,
                true,
                false,
                "podman",
                "build",
                "--tls-verify=false",
                ".");
    }

    @Test
    public void testBuildWithTag() throws MojoExecutionException, MavenFilteringException {
        configureMojo(false, false, false, DEFAULT_DOCKERFILE_DIR, "1.0.0", new String[]{"sampleTag"});

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        List<String> processOutput = List.of(imageHash);

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getFileFilterService()).thenReturn(fileFilterService);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);
        when(commandExecutorService.runCommand(
                isA(File.class),
                isA(Boolean.class),
                isA(Boolean.class),
                isA(String.class),
                isA(String.class),
                isA(String.class),
                isA(String.class)))
                .thenReturn(processOutput);

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(0)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container image " + imageHash + " as registry.example.com/sampleTag:1.0.0"));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));

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
        configureMojo(false, false, false, "src/test", "1.0.0", new String[]{"sampleTag"});

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildContextWithEmptyDockerfile() throws MojoExecutionException {
        configureMojo(false, false, false, "src/test/resources/emptydockerfile", "1.0.0", new String[]{"sampleTag"});

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    private void configureMojo(boolean skipAll, boolean skipBuild, boolean skipTag, String dockerFileDir, String tagVersion, String[] tags) {
        buildMojo.tlsVerify = TlsVerify.FALSE;
        buildMojo.skip = skipAll;
        buildMojo.skipAuth = true;
        buildMojo.skipBuild = skipBuild;
        buildMojo.skipTag = skipTag;
        buildMojo.dockerFileDir = dockerFileDir == null ? null : new File(dockerFileDir);
        buildMojo.outputDirectory = new File("target/podman-test");
        buildMojo.tagVersion = tagVersion;
        buildMojo.tags = tags;
        buildMojo.pushRegistry = "registry.example.com";
    }
}
