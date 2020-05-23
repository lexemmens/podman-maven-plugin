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
public class SaveMojoTest extends AbstractMojoTest {

    @InjectMocks
    private SaveMojo saveMojo;

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        configureMojo(true, false, null, null, false);

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipAuthenticationAndSave() throws MojoExecutionException {
        configureMojo(false, true, null, null, false);

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(1)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveNoTagVersionAndNoMavenProjectVersion() {
        configureMojo(false, false, null, null, true);

        Assertions.assertThrows(MojoExecutionException.class, saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveWithMavenProjectVersion() throws MojoExecutionException {
        configureMojo(false, false, "registry.example.com", new String[]{"sampleTag"}, true);

        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));

        File targetTestPodmanDir = new File("target/podman-test/podman");
        verify(commandExecutorService, times(1)).runCommand(new File(targetTestPodmanDir.getAbsolutePath()),
                "podman",
                "save",
                "",
                "--format",
                "oci-archive",
                "-o",
                "sampleTag_1_0_0.tar.gz",
                "registry.example.com/sampleTag:1.0.0");
    }

    private String normaliseImageName(String fullImageName) {
        String[] imageNameParts = fullImageName.split("\\/");
        String tagAndVersion = imageNameParts[imageNameParts.length - 1];
        return tagAndVersion.replaceAll("[\\.\\/\\-\\*:]", "_");
    }

    private void configureMojo(boolean skipAll, boolean skipSave, String targetRegistry, String[] tags, boolean useMavenProjectVersion) {
        saveMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        saveMojo.skip = skipAll;
        saveMojo.skipAuth = true;
        saveMojo.skipSave = skipSave;
        saveMojo.outputDirectory = new File("target/podman-test");
        saveMojo.targetRegistry = targetRegistry;
        saveMojo.useMavenProjectVersion = useMavenProjectVersion;
        saveMojo.tags = tags;
        saveMojo.targetRegistry = "registry.example.com";
    }
}
