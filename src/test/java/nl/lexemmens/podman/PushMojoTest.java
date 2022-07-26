package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.config.image.single.TestSingleImageConfigurationBuilder;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.podman.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.After;
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PushMojoTest extends AbstractMojoTest {

    @InjectMocks
    private PushMojo pushMojo;

    Map<String, String> testData = new HashMap<String, String>() {{
        put("podman-test", "push");
        put("podman-test-multistage", "push-multistage");
    }};

    @Before
    public void prepare() throws IOException {
        for (Map.Entry<String, String> entry : testData.entrySet()) {
            Path containerCatalogPath = Paths.get("target", entry.getKey(), "container-catalog.txt");
            Files.createDirectories(containerCatalogPath);
            Files.copy(
                    ClassLoader.getSystemClassLoader().getResourceAsStream(entry.getValue() + "/container-catalog.txt"),
                    containerCatalogPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test")
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
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test")
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
    public void testSkipPushWhenCatalogDoesNotExist() throws MojoExecutionException {
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setTags(null)
                .setUseMavenProjectVersion(false)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test-no-catalog");

        Assertions.assertThrows(MojoExecutionException.class, pushMojo::execute);

        verify(log, Mockito.times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, Mockito.times(1)).error(Mockito.eq("Failed to read container catalog. Make sure the build goal is executed."));
    }

    @Test
    public void testPushWithoutTargetRegistryFails() {
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, null, true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);

        Assertions.assertThrows(MojoExecutionException.class, pushMojo::execute);

        verify(log, times(1)).info(Mockito.eq("Registry authentication is skipped."));
        verify(log, times(0)).info(Mockito.eq("Pushing container images is skipped."));
        verify(log, times(0)).info(Mockito.eq("No tags specified. Will not push container images."));
        verify(log, times(1)).error(Mockito.eq("Failed to push container images. No registry specified. Configure the registry by adding the <pushRegistry><!-- registry --></pushRegistry> tag to your configuration."));

    }

    @Test
    public void testPushWithoutCleaningUpLocalImage() throws MojoExecutionException {
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
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
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
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
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
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
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
    public void testMultiStagePushOnlyFinalImage() throws MojoExecutionException {
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
    public void testMultiStagePushWithCustomTagPerStage() throws MojoExecutionException {
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(image, true, false, false, "registry.example.com", false, true, 0);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test-multistage");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 1);

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setTags(new String[]{})
                .setUseMavenProjectVersion(true)
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(image, true, false, false, "registry.example.com", true, true, 1);

        String targetRegistry = "registry.example.com/sample:1.0.0";
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
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
        List<SingleImageConfiguration> imageConfigurations = new ArrayList<>();
        imageConfigurations.add(image);
        pushMojo.images = imageConfigurations;
        pushMojo.failOnMissingContainerfile = failOnMissingContainerFile;
        pushMojo.retries = retries;
    }

    private static void cleanDir(Path dir) throws IOException {
        LinkedList<IOException> ioExceptions = new LinkedList<>();
        Files.list(dir).forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    cleanDir(path);
                }
                Files.delete(path);
            } catch (IOException e) {
                ioExceptions.add(e);
            }
        });
        if (!ioExceptions.isEmpty()) {
            IOException lastIOException = ioExceptions.removeLast();
            for (IOException ioException : ioExceptions) {
                ioException.printStackTrace();
            }
            throw lastIOException;
        }
    }

    @After
    public void cleanup() throws IOException {
        for (String directory : testData.keySet()) {
            cleanDir(Paths.get("target", directory));
        }
    }

}
