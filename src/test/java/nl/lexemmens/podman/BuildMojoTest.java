package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.config.image.single.TestSingleImageConfigurationBuilder;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.podman.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.service.ContainerfileDecorator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
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
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BuildMojoTest extends AbstractMojoTest {

    @InjectMocks
    private BuildMojo buildMojo;

    private ContainerfileDecorator containerfileDecorator;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        containerfileDecorator = new ContainerfileDecorator(log, mavenFileFilter, mavenProject);
    }

    private void verifyContainerCatalog(String... expectedImages) {
        List<String> actualImages = assertDoesNotThrow(() ->Files.lines(Paths.get(mavenProject.getBuild().getDirectory(), "container-catalog.txt")))
                .skip(1)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(expectedImages), actualImages, "Container catalog file must contain the expected images");
    }

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, false, true, false, false, true);

        buildMojo.execute();

        verify(log, Mockito.times(1)).info("Podman actions are skipped.");
    }

    @Test
    public void testSkipBuild() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, false, false, true, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        buildMojo.execute();

        verify(log, Mockito.times(1)).info("Building container images is skipped.");
    }

    @Test
    public void testBuildWithoutConfigThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        configureMojo(podman, null, true, false, false, true, true);

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testNoImageNameThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder(null).build();

        configureMojo(podman, image, true, false, false, true, true);

        try {
            buildMojo.execute();
            fail("Expected an exception, however none was thrown.");
        } catch (Exception e) {
            assertEquals("Image name must not be null, must be alphanumeric and may contain slashes, such as: valid/image/name", e.getMessage());
        }
    }

    @Test
    public void testNoStageConfigurationWhileCustomImageNameForStagesHasBeenSetToTrue() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder(null).build();

        configureMojo(podman, image, true, false, false, true, true);
        image.setCustomImageNameForMultiStageContainerfile(true);

        try {
            buildMojo.execute();
            fail("Expected an exception, however none was thrown.");
        } catch (Exception e) {
            assertEquals("Plugin is configured for multistage Containerfiles, but there are no custom image names configured.", e.getMessage());
        }
    }

    @Test
    public void testImageWithoutBuildSectionThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample-image").build();

        configureMojo(podman, image, true, false, false, true, true);
        image.setBuild(null);

        try {
            buildMojo.execute();
            fail("Expected an exception, however none was thrown.");
        } catch (Exception e) {
            assertEquals("Missing <build/> section in image configuration!", e.getMessage());
        }
    }

    @Test
    public void testBuildWithEmptyConfigsThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        configureMojo(podman, null, true, false, false, true, true);

        buildMojo.images = new ArrayList<>();

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildWithoutTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, true, false, false, true, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        buildMojo.execute();

        verify(log, Mockito.times(1)).info("Tagging container images is skipped.");
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(SingleImageConfiguration.class));
    }

    @Test
    public void testBuildWithoutPodmanConfigurationDoesNotThrowException() throws MojoExecutionException, MavenFilteringException {
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(null, image, true, false, false, true, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        Assertions.assertDoesNotThrow(() -> buildMojo.execute());

        verify(log, Mockito.times(1)).info("Tagging container images is skipped.");
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(SingleImageConfiguration.class));
    }

    @Test
    public void testBuildCustomContainerfileWithoutTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfile("Containerfile")
                .setContainerfileDir("src/test/resources/customdockerfile")
                .build();
        configureMojo(podman, image, true, false, false, true, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        buildMojo.execute();

        verify(log, Mockito.times(1)).info("Tagging container images is skipped.");
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(SingleImageConfiguration.class));
    }

    @Test
    public void testBuildAndTaggingWithNullTags() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("sha256:sampleimagehash"));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info("Tagging container images is skipped.");
        verify(log, Mockito.times(1)).info("No tags specified. Skipping tagging of container images.");
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(SingleImageConfiguration.class));
    }

    @Test
    public void testBuildAndTaggingWithEmptyTags() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .setTags(new String[]{})
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("sha256:sampleimagehash"));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info("Tagging container images is skipped.");
        verify(log, Mockito.times(1)).info("No tags specified. Skipping tagging of container images.");
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(SingleImageConfiguration.class));
    }

    @Test
    public void testBuildWithTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        String expectedFullImageName = "registry.example.com/sample:1.0.0";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList(imageHash));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info("Tagging container images is skipped.");
        verify(log, Mockito.times(0)).info("No tags specified. Skipping tagging of container images.");
        verify(log, Mockito.times(1)).info("Tagging container image " + imageHash + " as " + expectedFullImageName);
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(SingleImageConfiguration.class));
        verify(podmanExecutorService, times(1)).tag(imageHash, expectedFullImageName);
        verifyContainerCatalog(expectedFullImageName);
    }

    @Test
    public void testBuildWithLatestTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .setTags(new String[]{})
                .setCreateLatestTag(true)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        String expectedFullImageName = "registry.example.com/sample:latest";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList(imageHash));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info("Tagging container images is skipped.");
        verify(log, Mockito.times(0)).info("No tags specified. Skipping tagging of container images.");
        verify(log, Mockito.times(1)).info("Tagging container image " + imageHash + " as registry.example.com/sample:latest");
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));

        verify(podmanExecutorService, times(1)).build(isA(SingleImageConfiguration.class));
        verify(podmanExecutorService, times(1)).tag(imageHash, expectedFullImageName);
        verifyContainerCatalog(expectedFullImageName);
    }

    @Test
    public void testBuildContextNoContainerfile() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildContextWithoutContainerfileDoesNotThrowExceptionWhenConfigured() {
        String containerFileDir = "src/test/non-existing-directory";
        Path currentDir = Paths.get(".");
        Path targetLocation = currentDir.resolve(containerFileDir);
        String targetLocationAsString = targetLocation.normalize().toFile().getAbsolutePath();

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir(containerFileDir)
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);

        Assertions.assertDoesNotThrow(buildMojo::execute);
        verify(log, Mockito.times(1)).warn("No Containerfile was found at " + targetLocationAsString + File.separator + "Containerfile, however this will be ignored due to current plugin configuration.");
        verify(log, Mockito.times(1)).warn("Skipping build of container image with name sample. Configuration is not valid for this module!");
    }

    @Test
    public void testBuildContextWithDefaultContainerfileDir() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(mavenProject.getBasedir()).thenReturn(new File("."));
        when(build.getDirectory()).thenReturn("target");

        // No Containerfile present at root level
        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildContextWithCustomPodmanRunDirectory() {
        String runDirectory = "customDir";
        Path currentDir = Paths.get(".");
        Path targetRunDir = currentDir.resolve(runDirectory);
        String targetRunDirAsString = targetRunDir.normalize().toFile().getAbsolutePath();

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder()
                .setTlsVerify(TlsVerify.FALSE)
                .setRunDirectory(new File(runDirectory))
                .build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(mavenProject.getBasedir()).thenReturn(new File("."));
        when(build.getDirectory()).thenReturn("target");

        // No Containerfile present at root level
        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);

        verify(log, times(1)).info("Setting Podman's run directory " + targetRunDirAsString);
    }

    @Test
    public void testBuildContextWithEmptyContainerfile() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/emptydockerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testMultiStageBuildTagOnlyFinalImage() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = PushMojoTest.class.getResource("/multistagecontainerfile/samplebuildoutput.txt").toURI();
        Path sampleBuildOutputPath = Paths.get(sampleBuildOutputUri);

        List<String> buildOutputUnderTest;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info("Detected multistage Containerfile...");

        // At random verify some lines
        verify(log, times(1)).debug("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base'");
        verify(log, times(1)).debug("Processing candidate: 'STEP 7: LABEL Build-User=sample-user2 Git-Repository-Url=null'");

        // Verify stage detection
        verify(log, times(1)).debug("Processing stage in Containerfile: base");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase2");

        // Verify hashes for stages
        verify(log, times(1)).info("Final image for stage base is: 7e72c870614");
        verify(log, times(1)).info("Final image for stage phase is: 7f55eab001a");
        verify(log, times(1)).info("Final image for stage phase2 is: d2efc6645cb");

        // Verify tagging image
        verify(log, times(1)).info("Tagging container image d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f as registry.example.com/sample:1.0.0");
        verify(podmanExecutorService, times(1)).tag("d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f", "registry.example.com/sample:1.0.0");

        verify(log, times(1)).info("Built container image.");
        verifyContainerCatalog("registry.example.com/sample:1.0.0");
    }

    @Test
    public void testMultiStageBuildWithCustomTagPerStage() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = PushMojoTest.class.getResource("/multistagecontainerfile/samplebuildoutput.txt").toURI();
        Path sampleBuildOutputPath = Paths.get(sampleBuildOutputUri);

        List<String> buildOutputUnderTest;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info("Detected multistage Containerfile...");

        // At random verify some lines
        verify(log, times(1)).debug("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base'");
        verify(log, times(1)).debug("Processing candidate: 'STEP 7: LABEL Build-User=sample-user2 Git-Repository-Url=null'");

        // Verify stage detection
        verify(log, times(1)).debug("Processing stage in Containerfile: base");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase2");

        // Verify hashes for stages
        verify(log, times(1)).info("Final image for stage base is: 7e72c870614");
        verify(log, times(1)).info("Final image for stage phase is: 7f55eab001a");
        verify(log, times(1)).info("Final image for stage phase2 is: d2efc6645cb");

        // Verify tagging image
        verify(log, times(1)).info("Tagging container image 7f55eab001a from stage phase as registry.example.com/image-name-number-1:0.2.1");
        verify(log, times(1)).info("Tagging container image d2efc6645cb from stage phase2 as registry.example.com/image-name-number-2:0.2.1");
        verify(log, times(0)).info("Tagging container image d2efc6645cb as registry.example.com/sample:1.0.0");

        verify(podmanExecutorService, times(1)).tag("7f55eab001a", "registry.example.com/image-name-number-1:0.2.1");
        verify(podmanExecutorService, times(1)).tag("d2efc6645cb", "registry.example.com/image-name-number-2:0.2.1");

        verify(log, times(1)).info("Built container image.");

        verifyContainerCatalog(
                "registry.example.com/image-name-number-1:0.2.1",
                "registry.example.com/image-name-number-2:0.2.1"
        );
    }

    @Test
    public void testMultiStageBuildWithCustomTagPerStageButStageNameIsNotConfigured() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = PushMojoTest.class.getResource("/multistagecontainerfile/samplebuildoutput.txt").toURI();
        Path sampleBuildOutputPath = Paths.get(sampleBuildOutputUri);

        List<String> buildOutputUnderTest;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("non-existent-phase", "image-name-number-1")
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info("Detected multistage Containerfile...");

        // At random verify some lines
        verify(log, times(1)).debug("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base'");
        verify(log, times(1)).debug("Processing candidate: 'STEP 7: LABEL Build-User=sample-user2 Git-Repository-Url=null'");

        // Verify stage detection
        verify(log, times(1)).debug("Processing stage in Containerfile: base");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase2");

        // Verify hashes for stages
        verify(log, times(1)).info("Final image for stage base is: 7e72c870614");
        verify(log, times(1)).info("Final image for stage phase is: 7f55eab001a");
        verify(log, times(1)).info("Final image for stage phase2 is: d2efc6645cb");

        // Verify warning markers
        verify(log, times(1)).warn("No image name configured for build stage: phase. Image 7f55eab001a not tagged!");
        verify(log, times(1)).warn("No image name configured for build stage: phase2. Image d2efc6645cb not tagged!");

        verify(log, times(1)).info("Built container image.");
    }

    @Test
    public void testMultiStageContainerFileWithMultilineOutputStep() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = PushMojoTest.class.getResource("/multistagecontainerfile/samplebuildoutput_multiline_step.txt").toURI();
        Path sampleBuildOutputPath = Paths.get(sampleBuildOutputUri);

        List<String> buildOutputUnderTest;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info("Detected multistage Containerfile...");

        // At random verify some lines
        verify(log, times(1)).debug("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base'");
        verify(log, times(1)).debug("Processing candidate: 'STEP 8: LABEL Build-User=sample-user2 Git-Repository-Url=null'");

        // Verify stage detection
        verify(log, times(1)).debug("Processing stage in Containerfile: base");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase2");

        // Verify hashes for stages
        verify(log, times(1)).info("Final image for stage base is: 7e72c870614");
        verify(log, times(1)).info("Final image for stage phase is: 7f55eab001a");
        verify(log, times(1)).info("Final image for stage phase2 is: d2efc6645cb");

        // Verify tagging image
        verify(log, times(1)).info("Tagging container image 7f55eab001a from stage phase as registry.example.com/image-name-number-1:0.2.1");
        verify(log, times(1)).info("Tagging container image d2efc6645cb from stage phase2 as registry.example.com/image-name-number-2:0.2.1");
        verify(log, times(0)).info("Tagging container image d2efc6645cb as registry.example.com/sample:1.0.0");

        verify(podmanExecutorService, times(1)).tag("7f55eab001a", "registry.example.com/image-name-number-1:0.2.1");
        verify(podmanExecutorService, times(1)).tag("d2efc6645cb", "registry.example.com/image-name-number-2:0.2.1");

        verify(log, times(1)).info("Built container image.");

        verifyContainerCatalog(
                "registry.example.com/image-name-number-1:0.2.1",
                "registry.example.com/image-name-number-2:0.2.1"
        );
    }

    @Test
    public void testMultiStageBuildWithCustomTagPerStageFinalLineDifferent() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = PushMojoTest.class.getResource("/multistagecontainerfile/samplebuildoutput2.txt").toURI();
        Path sampleBuildOutputPath = Paths.get(sampleBuildOutputUri);

        List<String> buildOutputUnderTest;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info("Detected multistage Containerfile...");

        // At random verify some lines
        verify(log, times(1)).debug("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base'");
        verify(log, times(1)).debug("Processing candidate: 'STEP 7: LABEL Build-User=sample-user2 Git-Repository-Url=null'");

        // Verify stage detection
        verify(log, times(1)).debug("Processing stage in Containerfile: base");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase2");

        // Verify hashes for stages
        verify(log, times(1)).info("Final image for stage base is: 7e72c870614");
        verify(log, times(1)).info("Final image for stage phase is: 7f55eab001a");
        verify(log, times(1)).info("Final image for stage phase2 is: d2efc6645cb");

        // Verify tagging image
        verify(log, times(1)).info("Tagging container image 7f55eab001a from stage phase as registry.example.com/image-name-number-1:0.2.1");
        verify(log, times(1)).info("Tagging container image d2efc6645cb from stage phase2 as registry.example.com/image-name-number-2:0.2.1");

        verify(podmanExecutorService, times(1)).tag("7f55eab001a", "registry.example.com/image-name-number-1:0.2.1");
        verify(podmanExecutorService, times(1)).tag("d2efc6645cb", "registry.example.com/image-name-number-2:0.2.1");

        verify(log, times(1)).info("Built container image.");

        verifyContainerCatalog(
                "registry.example.com/image-name-number-1:0.2.1",
                "registry.example.com/image-name-number-2:0.2.1"
        );
    }

    @Test
    public void testMultiStageBuildWithCustomTagPerStagePodman1x() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = PushMojoTest.class.getResource("/multistagecontainerfile/samplebuildoutput_podman1x.txt").toURI();
        Path sampleBuildOutputPath = Paths.get(sampleBuildOutputUri);

        List<String> buildOutputUnderTest;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"0.2.1"})
                .setCreateLatestTag(false)
                .setUseCustomImageNameForMultiStageContainerfile(true)
                .addCustomImageNameForBuildStage("phase", "image-name-number-1")
                .addCustomImageNameForBuildStage("phase2", "image-name-number-2")
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(serviceHub.getMavenProjectHelper()).thenReturn(mavenProjectHelper);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info("Detected multistage Containerfile...");

        // At random verify some lines
        verify(log, times(1)).debug("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base'");
        verify(log, times(1)).debug("Processing candidate: 'STEP 7: LABEL Build-User=sample-user2 Git-Repository-Url=null'");

        // Verify stage detection
        verify(log, times(1)).debug("Processing stage in Containerfile: base");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase");
        verify(log, times(1)).debug("Processing stage in Containerfile: phase2");

        // Verify hashes for stages
        verify(log, times(1)).info("Final image for stage base is: 823ed30df4abf7574498d5f766b0ebf70793d73a99fc4220dda200c5131b60ce");
        verify(log, times(1)).info("Final image for stage phase is: b51d6faa80bc4cc9ea93ec3b3b3bdff9629500330df37295c72d388d17b9c303");
        verify(log, times(1)).info("Final image for stage phase2 is: ba6cb6863b48c3487810458db4b88b238f086cef65078839d9efe30f1069bed7");

        // Verify tagging image
        verify(log, times(1)).info("Tagging container image b51d6faa80bc4cc9ea93ec3b3b3bdff9629500330df37295c72d388d17b9c303 from stage phase as registry.example.com/image-name-number-1:0.2.1");
        verify(log, times(1)).info("Tagging container image ba6cb6863b48c3487810458db4b88b238f086cef65078839d9efe30f1069bed7 from stage phase2 as registry.example.com/image-name-number-2:0.2.1");

        verify(podmanExecutorService, times(1)).tag("b51d6faa80bc4cc9ea93ec3b3b3bdff9629500330df37295c72d388d17b9c303", "registry.example.com/image-name-number-1:0.2.1");
        verify(podmanExecutorService, times(1)).tag("ba6cb6863b48c3487810458db4b88b238f086cef65078839d9efe30f1069bed7", "registry.example.com/image-name-number-2:0.2.1");

        verify(log, times(1)).info("Built container image.");

        verifyContainerCatalog(
                "registry.example.com/image-name-number-1:0.2.1",
                "registry.example.com/image-name-number-2:0.2.1"
        );
    }

    private void configureMojo(PodmanConfiguration podman, SingleImageConfiguration image, boolean skipAuth, boolean skipAll, boolean skipBuild, boolean skipTag, boolean failOnMissingContainerFile) {
        buildMojo.podman = podman;
        buildMojo.skip = skipAll;
        buildMojo.skipBuild = skipBuild;
        buildMojo.skipAuth = skipAuth;
        buildMojo.skipTag = skipTag;
        if(image == null) {
            buildMojo.images = null;
        } else {
            List<SingleImageConfiguration> imageConfigurations = new ArrayList<>();
            imageConfigurations.add(image);
            buildMojo.images = imageConfigurations;
        }
        buildMojo.pushRegistry = "registry.example.com";
        buildMojo.failOnMissingContainerfile = failOnMissingContainerFile;
    }
}
