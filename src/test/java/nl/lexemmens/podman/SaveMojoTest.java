package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
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
        saveMojo.skip = true;
        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipAuthenticationAndSave() throws MojoExecutionException {
        saveMojo.skipAuth = true;
        saveMojo.skipSave = true;
        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(1)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveNoTagVersionAndNoMavenProjectVersion() throws MojoExecutionException {
        saveMojo.skipAuth = true;
        saveMojo.useMavenProjectVersion = false;
        saveMojo.tagVersion = null;

        Assertions.assertThrows(MojoExecutionException.class, saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveWithMavenProjectVersion() throws MojoExecutionException {
        saveMojo.tlsVerify = TlsVerify.NOT_SPECIFIED;
        saveMojo.skipAuth = true;
        saveMojo.useMavenProjectVersion = true;
        saveMojo.tagVersion = null;
        saveMojo.tags = new String[]{"sampleTag"};
        saveMojo.targetRegistry = "registry.example.com";
        saveMojo.outputDirectory = new File("./target/podman-test");

        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenFileFilter.class), isA(TlsVerify.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getCommandExecutorService()).thenReturn(commandExecutorService);

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));

        File targetTestPodmanDir = new File("./target/podman-test/podman");
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
}
