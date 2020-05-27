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

import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class SaveMojoTest extends AbstractMojoTest {

    @InjectMocks
    private SaveMojo saveMojo;

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        configureMojo(true, false, null,  false);

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipSave() throws MojoExecutionException {
        configureMojo(false, true, null,  false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveNoTags() {
        configureMojo(false, false, null,  false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveWithMavenProjectVersion() throws MojoExecutionException {
        configureMojo(false, false, "registry.example.com",  true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
        verify(podmanExecutorService, times(1)).save(eq("sample_1_0_0.tar.gz"), eq("registry.example.com/sample:1.0.0"));
    }

    @Test
    public void testSaveImageFromLocalRegistry() throws MojoExecutionException {
        configureMojo(false, false, null,  true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
        verify(podmanExecutorService, times(1)).save(eq("sample_1_0_0.tar.gz"), eq("sample:1.0.0"));
    }

    private void configureMojo(boolean skipAll, boolean skipSave, String pushRegistry, boolean useMavenProjectVersion) {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(useMavenProjectVersion)
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .build();

        List<ImageConfiguration> images = List.of(image);

        saveMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        saveMojo.skip = skipAll;
        saveMojo.skipAuth = true;
        saveMojo.skipSave = skipSave;
        saveMojo.pushRegistry = pushRegistry;
        saveMojo.images = images;
    }
}
