package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.TestImageConfigurationBuilder;
import nl.lexemmens.podman.config.TestPodmanConfigurationBuilder;
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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, null, true);

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipSave() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, false, true, null, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        saveMojo.execute();

        verify(log, times(1)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveNoTags() {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image,false, false, null, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        Assertions.assertDoesNotThrow(saveMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Saving container images is skipped."));
    }

    @Test
    public void testSaveWithoutContainerfileDoesNotThrowExceptionWhenConfigured() {
        String containerFileDir = "src/test/non-existing-directory";
        Path currentDir = Paths.get(".");
        Path targetLocation = currentDir.resolve(containerFileDir);
        String targetLocationAsString = targetLocation.normalize().toFile().getAbsolutePath();

        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(targetLocationAsString)
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(image, false, false, "registry.example.com", false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Assertions.assertDoesNotThrow(saveMojo::execute);
        verify(log, Mockito.times(1)).warn(Mockito.eq("No Containerfile was found at " + targetLocationAsString + File.separator + "Containerfile, however this will be ignored due to current plugin configuration."));
        verify(log, Mockito.times(1)).warn(Mockito.eq("Skipping save of container image with name sample. Configuration is not valid for this module!"));
    }

    @Test
    public void testSaveWithMavenProjectVersion() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image,false, false, "registry.example.com", true);

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
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, false, false, null, true);

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
        Path target = Paths.get(".", "target", "podman");

        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(image, false, false, "registry.example.com", true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        saveMojo.execute();

        // Verify logging
        verify(log, times(1)).info(Mockito.eq("Exporting container images to local disk ..."));
        verify(log, times(1)).warn(Mockito.eq("Detected multistage Containerfile, but no custom image names have been specified. Falling back to exporting final image."));
        verify(log, times(1)).info(Mockito.eq("Exporting image registry.example.com/sample:1.0.0 to " + target.resolve("sample_1_0_0.tar.gz").normalize().toFile().getAbsolutePath()));
        verify(podmanExecutorService, times(1)).save(eq("sample_1_0_0.tar.gz"), eq("registry.example.com/sample:1.0.0"));
        verify(log, times(1)).info(Mockito.eq("Container images exported successfully."));
    }

    @Test
    public void testMultiStageSaveWithCustomTagPerStage() throws MojoExecutionException, IOException, URISyntaxException {
        Path target = Paths.get(".", "target", "podman");

        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(image, false, false, "registry.example.com", true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        saveMojo.execute();

        // Verify logging
        verify(log, times(1)).info(Mockito.eq("Exporting container images to local disk ..."));
        verify(log, times(0)).warn(Mockito.eq("Detected multistage Containerfile, but no custom image names have been specified. Falling back to exporting final image."));
        verify(log, times(1)).info(Mockito.eq("Exporting image registry.example.com/image-name-number-1:0.2.1 to " + target.resolve("image_name_number_1_0_2_1.tar.gz").normalize().toFile().getAbsolutePath()));
        verify(podmanExecutorService, times(1)).save(eq("image_name_number_1_0_2_1.tar.gz"), eq("registry.example.com/image-name-number-1:0.2.1"));
        verify(log, times(1)).info(Mockito.eq("Exporting image registry.example.com/image-name-number-2:0.2.1 to " + target.resolve("image_name_number_2_0_2_1.tar.gz").normalize().toFile().getAbsolutePath()));
        verify(podmanExecutorService, times(1)).save(eq("image_name_number_2_0_2_1.tar.gz"), eq("registry.example.com/image-name-number-2:0.2.1"));
        verify(log, times(1)).info(Mockito.eq("Container images exported successfully."));
    }

    private void configureMojo(SingleImageConfiguration image, boolean skipAll, boolean skipSave, String pushRegistry, boolean failOnMissingContainerfile) {
        List<SingleImageConfiguration> images = List.of(image);

        saveMojo.podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.NOT_SPECIFIED).build();
        saveMojo.skip = skipAll;
        saveMojo.skipAuth = true;
        saveMojo.skipSave = skipSave;
        saveMojo.pushRegistry = pushRegistry;
//        saveMojo.images = images;
        saveMojo.failOnMissingContainerfile = failOnMissingContainerfile;
    }
}