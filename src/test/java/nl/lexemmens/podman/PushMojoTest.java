package nl.lexemmens.podman;

import nl.lexemmens.podman.config.TestImageConfigurationBuilder;
import nl.lexemmens.podman.config.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.enumeration.TlsVerify;
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

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PushMojoTest extends AbstractMojoTest {

    @InjectMocks
    private PushMojo pushMojo;

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("test")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, false, true, false, null, false, true, 0);

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipPush() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("test")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, true, "registry.example.com", true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Pushing container images is skipped."));
    }

    @Test
    public void testSkipPushWhenTagsNull() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(null)
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container image named sample"));
    }

    @Test
    public void testPushWhenNoContainerFileIsPresentAndNoExceptionIsThrown() {
        String containerFileDir = "src/test/non-existing-directory";
        Path currentDir = Paths.get(".");
        Path targetLocation = currentDir.resolve(containerFileDir);
        String targetLocationAsString = targetLocation.normalize().toFile().getAbsolutePath();

        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(containerFileDir)
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(image, true, false, false, "registry.sample.com", true, false, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Assertions.assertDoesNotThrow(pushMojo::execute);
        verify(log, Mockito.times(1)).warn(Mockito.eq("No Containerfile was found at " + targetLocationAsString + File.separator + "Containerfile, however this will be ignored due to current plugin configuration."));
        verify(log, Mockito.times(1)).warn(Mockito.eq("Skipping push of container image with name sample. Configuration is not valid for this module!"));
    }

    @Test
    public void testSkipPushWhenTagsEmpty() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        pushMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Will not push container image named sample"));
    }

    @Test
    public void testPushWithoutTargetRegistryFails() {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, null, true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);

        Assertions.assertThrows(MojoExecutionException.class, pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(log, times(1)).error(Mockito.eq("Failed to push container images. No registry specified. Configure the registry by adding the <pushRegistry><!-- registry --></pushRegistry> tag to your configuration."));

    }



    @Test
    public void testPushWithoutCleaningUpLocalImage() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        doNothing().when(podmanExecutorService).push(eq(targetRegistry));

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(podmanExecutorService, times(1)).push(eq(targetRegistry));
    }

    @Test
    public void testPushWithValidAuthentication() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        pushMojo.registries = new String[]{"registries.example.com"};
        pushMojo.skipAuth = false;

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        doNothing().when(podmanExecutorService).push(eq(targetRegistry));
        when(serviceHub.getAuthenticationService()).thenReturn(authenticationService);

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(0)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(podmanExecutorService, times(1)).push(eq(targetRegistry));
    }

    @Test
    public void testPushWithValidAuthenticationPrintPodmanVersion() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        pushMojo.registries = new String[]{"registries.example.com"};
        pushMojo.skipAuth = false;

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(log.isDebugEnabled()).thenReturn(true);
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        doNothing().when(podmanExecutorService).push(eq(targetRegistry));
        when(serviceHub.getAuthenticationService()).thenReturn(authenticationService);

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(0)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(podmanExecutorService, times(1)).push(eq(targetRegistry));
        verify(podmanExecutorService, times(1)).version();
    }

    @Test
    public void testPushWithCleaningUpLocalImage() throws MojoExecutionException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        pushMojo.deleteLocalImageAfterPush = true;

        String imageName = "registry.example.com/sample:1.0.0";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        doNothing().when(podmanExecutorService).push(eq(imageName));
        doNothing().when(podmanExecutorService).removeLocalImage(eq(imageName));

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(log, times(1)).info(Mockito.eq("Removing image " + imageName + " from the local repository"));
        verify(podmanExecutorService, times(1)).push(eq(imageName));
        verify(podmanExecutorService, times(1)).removeLocalImage(eq(imageName));
    }

    @Test
    public void testMultiStagePushOnlyFinalImage() throws MojoExecutionException, IOException, URISyntaxException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        pushMojo.execute();

        // Verify logging
        verify(log, times(1)).info(Mockito.eq("Pushing container images to registry ..."));
        verify(log, times(1)).info(Mockito.eq("Pushing image: registry.example.com/sample:1.0.0 to registry.example.com"));
        verify(podmanExecutorService, times(1)).push(eq("registry.example.com/sample:1.0.0"));
        verify(log, times(1)).info(Mockito.eq("Successfully pushed container image registry.example.com/sample:1.0.0 to registry.example.com"));
        verify(log, times(1)).info(Mockito.eq("All images have been successfully pushed to the registry"));
    }

    @Test
    public void testMultiStagePushWithCustomTagPerStage() throws MojoExecutionException, IOException, URISyntaxException {
        SingleImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(image, true, false, false, "registry.example.com", false, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        pushMojo.execute();

        /// Verify logging
        verify(log, times(1)).info(Mockito.eq("Pushing container images to registry ..."));

        verify(log, times(1)).info(Mockito.eq("Pushing image: registry.example.com/image-name-number-1:0.2.1 to registry.example.com"));
        verify(podmanExecutorService, times(1)).push(eq("registry.example.com/image-name-number-1:0.2.1"));
        verify(log, times(1)).info(Mockito.eq("Successfully pushed container image registry.example.com/image-name-number-1:0.2.1 to registry.example.com"));

        verify(log, times(1)).info(Mockito.eq("Pushing image: registry.example.com/image-name-number-2:0.2.1 to registry.example.com"));
        verify(podmanExecutorService, times(1)).push(eq("registry.example.com/image-name-number-2:0.2.1"));
        verify(log, times(1)).info(Mockito.eq("Successfully pushed container image registry.example.com/image-name-number-2:0.2.1 to registry.example.com"));

        verify(log, times(1)).info(Mockito.eq("All images have been successfully pushed to the registry"));
    }

    @Test
    public void testPushWithRetries() throws MojoExecutionException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 1);

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        // Simulate failure
        doThrow(new MojoExecutionException("Execution failed"))
                .doNothing()
                .when(podmanExecutorService).push(eq(targetRegistry));

        Assertions.assertDoesNotThrow(pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(podmanExecutorService, times(2)).push(eq(targetRegistry));
    }

    @Test
    public void testPushRunsOutOfRetries() throws MojoExecutionException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 1);

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);

        // Simulate failure
        doThrow(new MojoExecutionException("Execution failed")).when(podmanExecutorService).push(eq(targetRegistry));

        Assertions.assertThrows(MojoExecutionException.class, pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(podmanExecutorService, times(2)).push(eq(targetRegistry));
    }

    private void configureMojo(SingleImageConfiguration image, boolean skipAuth, boolean skipAll, boolean skipPush, String targetRegistry, boolean deleteLocalImageAfterPush, boolean failOnMissingContainerFile, int retries) {
        pushMojo.podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.NOT_SPECIFIED).build();
        pushMojo.skip = skipAll;
        pushMojo.skipAuth = skipAuth;
        pushMojo.skipPush = skipPush;
        pushMojo.pushRegistry = targetRegistry;
        pushMojo.registries = new String[]{targetRegistry};
        pushMojo.deleteLocalImageAfterPush = deleteLocalImageAfterPush;
        SingleImageConfiguration[] imageConfigurations = new SingleImageConfiguration[1];
        imageConfigurations[0] = image;
//        pushMojo.images = imageConfigurations;
        pushMojo.failOnMissingContainerfile = failOnMissingContainerFile;
        pushMojo.retries = retries;
    }

}
