package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.image.PodmanConfiguration;
import nl.lexemmens.podman.image.TestImageConfigurationBuilder;
import nl.lexemmens.podman.image.TestPodmanConfigurationBuilder;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class SaveMojoTest extends AbstractMojoTest {

    @InjectMocks
    private SaveMojo saveMojo;

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, null);

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipSave() throws MojoExecutionException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, false, true, null);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveNoTags() {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image,false, false, null);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveWithMavenProjectVersion() throws MojoExecutionException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image,false, false, "registry.example.com");

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
        verify(podmanExecutorService, times(1)).save(eq("sample_1_0_0.tar.gz"), eq("registry.example.com/sample:1.0.0"));
    }

    @Test
    public void testSaveImageFromLocalRegistry() throws MojoExecutionException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, false, false, null);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
        verify(podmanExecutorService, times(1)).save(eq("sample_1_0_0.tar.gz"), eq("sample:1.0.0"));
    }

    @Test
    public void testMultiStageSaveOnlyFinalImage() throws MojoExecutionException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(image, false, false, "registry.example.com");

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        saveMojo.execute();

        // Verify logging
        verify(log, times(1)).info(Mockito.eq("Exporting container images to local disk ..."));
        verify(log, times(1)).warn(Mockito.eq("Detected multistage Containerfile, but no custom image names have been specified. Falling back to exporting final image."));
        verify(log, times(1)).info(Mockito.eq("Exporting image registry.example.com/sample:1.0.0 to /home/lexemmens/Projects/podman-maven-plugin/target/podman/sample_1_0_0.tar.gz"));
        verify(podmanExecutorService, times(1)).save(eq("sample_1_0_0.tar.gz"), eq("registry.example.com/sample:1.0.0"));
        verify(log, times(1)).info(Mockito.eq("Container images exported successfully."));
    }

    @Test
    public void testMultiStageSaveWithCustomTagPerStage() throws MojoExecutionException, IOException, URISyntaxException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(image, false, false, "registry.example.com");

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        saveMojo.execute();

        // Verify logging
        verify(log, times(1)).info(Mockito.eq("Exporting container images to local disk ..."));
        verify(log, times(0)).warn(Mockito.eq("Detected multistage Containerfile, but no custom image names have been specified. Falling back to exporting final image."));
        verify(log, times(1)).info(Mockito.eq("Exporting image registry.example.com/image-name-number-1:0.2.1 to /home/lexemmens/Projects/podman-maven-plugin/target/podman/image_name_number_1_0_2_1.tar.gz"));
        verify(podmanExecutorService, times(1)).save(eq("image_name_number_1_0_2_1.tar.gz"), eq("registry.example.com/image-name-number-1:0.2.1"));
        verify(log, times(1)).info(Mockito.eq("Exporting image registry.example.com/image-name-number-2:0.2.1 to /home/lexemmens/Projects/podman-maven-plugin/target/podman/image_name_number_2_0_2_1.tar.gz"));
        verify(podmanExecutorService, times(1)).save(eq("image_name_number_2_0_2_1.tar.gz"), eq("registry.example.com/image-name-number-2:0.2.1"));
        verify(log, times(1)).info(Mockito.eq("Container images exported successfully."));
    }

    private void configureMojo(ImageConfiguration image, boolean skipAll, boolean skipSave, String pushRegistry) {
        List<ImageConfiguration> images = List.of(image);

        saveMojo.podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.NOT_SPECIFIED).build();
        saveMojo.skip = skipAll;
        saveMojo.skipAuth = true;
        saveMojo.skipSave = skipSave;
        saveMojo.pushRegistry = pushRegistry;
        saveMojo.images = images;
    }
}