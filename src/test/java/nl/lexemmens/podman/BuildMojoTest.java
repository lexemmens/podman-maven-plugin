package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.image.PodmanConfiguration;
import nl.lexemmens.podman.image.TestImageConfigurationBuilder;
import nl.lexemmens.podman.image.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.service.ContainerfileDecorator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BuildMojoTest extends AbstractMojoTest {

    @InjectMocks
    private BuildMojo buildMojo;

    private ContainerfileDecorator containerfileDecorator;

    @Before
    public void setup() {
        initMocks(this);

        containerfileDecorator = new ContainerfileDecorator(log, mavenFileFilter, mavenProject);
    }

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, false, true, false, false, true);

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipBuild() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, false, false, true, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Building container images is skipped."));
    }

    @Test
    public void testBuildWithoutConfigThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        configureMojo(podman, null, true, false, false, true, true);

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
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
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, true, false, false, true, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container images is skipped."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
    }

    @Test
    public void testBuildWithoutPodmanConfigurationDoesNotThrowException() throws MojoExecutionException, MavenFilteringException {
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(null, image, true, false, false, true, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        Assertions.assertDoesNotThrow(() -> buildMojo.execute());

        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container images is skipped."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
    }

    @Test
    public void testBuildCustomContainerfileWithoutTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfile("Containerfile")
                .setContainerfileDir("src/test/resources/customdockerfile")
                .build();
        configureMojo(podman, image, true, false, false, true, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container images is skipped."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
    }

    @Test
    public void testBuildAndTaggingWithNullTags() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of("sha256:sampleimagehash"));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
    }

    @Test
    public void testBuildAndTaggingWithEmptyTags() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .setTags(new String[]{})
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of("sha256:sampleimagehash"));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(1)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
    }

    @Test
    public void testBuildWithTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        String expectedFullImageName = "registry.example.com/sample:1.0.0";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of(imageHash));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(0)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container image " + imageHash + " as " + expectedFullImageName));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
        verify(podmanExecutorService, times(1)).tag(eq(imageHash), eq(expectedFullImageName));
    }

    @Test
    public void testBuildWithLatestTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir(DEFAULT_CONTAINERFILE_DIR)
                .setTags(new String[]{})
                .setCreateLatestTag(true)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        String expectedFullImageName = "registry.example.com/sample:latest";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of(imageHash));

        buildMojo.execute();

        verify(log, Mockito.times(0)).info(Mockito.eq("Tagging container images is skipped."));
        verify(log, Mockito.times(0)).info(Mockito.eq("No tags specified. Skipping tagging of container images."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container image " + imageHash + " as registry.example.com/sample:latest"));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));

        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
        verify(podmanExecutorService, times(1)).tag(eq(imageHash), eq(expectedFullImageName));
    }

    @Test
    public void testBuildContextNoContainerfile() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
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
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/non-existing-directory")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Assertions.assertDoesNotThrow(buildMojo::execute);
        verify(log, Mockito.times(1)).warn(Mockito.eq("No Containerfile was found at /home/lexemmens/Projects/podman-maven-plugin/src/test/non-existing-directory/Containerfile, however this will be ignored due to current plugin configuration."));
        verify(log, Mockito.times(1)).warn(Mockito.eq("Skipping build of container image with name sample. Configuration is not valid for this module!"));
    }

    @Test
    public void testBuildContextWithDefaultContainerfileDir() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
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
    public void testBuildContextWithEmptyContainerfile() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
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

        List<String> buildOutputUnderTest = null;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setContainerfileDir("src/test/resources/multistagecontainerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info(Mockito.eq("Detected multistage Containerfile..."));

        // At random verify some lines
        verify(log, times(1)).debug(Mockito.eq("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base', matches: true"));
        verify(log, times(1)).debug(Mockito.eq("Processing line: 'STEP 7: LABEL Build-User=sample-user2 Git-Repository-Url=null', matches: false"));

        // Verify stage detection
        verify(log, times(1)).debug(Mockito.eq("Initial detection of a stage in Containerfile. Stage: base"));
        verify(log, times(1)).debug(Mockito.eq("Found new stage in Containerfile: phase"));
        verify(log, times(1)).debug(Mockito.eq("Found new stage in Containerfile: phase2"));

        // Verify hashes for stages
        verify(log, times(1)).info(Mockito.eq("Found image hash 7e72c870614 for stage base"));
        verify(log, times(1)).info(Mockito.eq("Found image hash 7f55eab001a for stage phase"));
        verify(log, times(1)).info(Mockito.eq("Using image hash of final image (d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f) for stage: phase2"));

        // Verify tagging image
        verify(log, times(1)).info(Mockito.eq("Tagging container image d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f as registry.example.com/sample:1.0.0"));
        verify(podmanExecutorService, times(1)).tag(eq("d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f"), eq("registry.example.com/sample:1.0.0"));

        verify(log, times(1)).info(Mockito.eq("Built container image."));
    }

    @Test
    public void testMultiStageBuildWithCustomTagPerStage() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = PushMojoTest.class.getResource("/multistagecontainerfile/samplebuildoutput.txt").toURI();
        Path sampleBuildOutputPath = Paths.get(sampleBuildOutputUri);

        List<String> buildOutputUnderTest = null;
        try (Stream<String> buildSampleOutput = Files.lines(sampleBuildOutputPath)) {
            buildOutputUnderTest = buildSampleOutput.collect(Collectors.toList());
        }

        Assertions.assertNotNull(buildOutputUnderTest);

        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
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
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(buildOutputUnderTest);

        buildMojo.execute();

        // Verify logging
        verify(log, times(1)).info(Mockito.eq("Detected multistage Containerfile..."));

        // At random verify some lines
        verify(log, times(1)).debug(Mockito.eq("Processing line: 'STEP 1: FROM nexus.example:15000/adoptopenjdk/openjdk11:11.0.3 AS base', matches: true"));
        verify(log, times(1)).debug(Mockito.eq("Processing line: 'STEP 7: LABEL Build-User=sample-user2 Git-Repository-Url=null', matches: false"));

        // Verify stage detection
        verify(log, times(1)).debug(Mockito.eq("Initial detection of a stage in Containerfile. Stage: base"));
        verify(log, times(1)).debug(Mockito.eq("Found new stage in Containerfile: phase"));
        verify(log, times(1)).debug(Mockito.eq("Found new stage in Containerfile: phase2"));

        // Verify hashes for stages
        verify(log, times(1)).info(Mockito.eq("Found image hash 7e72c870614 for stage base"));
        verify(log, times(1)).info(Mockito.eq("Found image hash 7f55eab001a for stage phase"));
        verify(log, times(1)).info(Mockito.eq("Using image hash of final image (d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f) for stage: phase2"));

        // Verify tagging image
        verify(log, times(1)).info(Mockito.eq("Tagging container image 7f55eab001a from stage phase as registry.example.com/image-name-number-1:0.2.1"));
        verify(log, times(1)).info(Mockito.eq("Tagging container image d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f from stage phase2 as registry.example.com/image-name-number-2:0.2.1"));
        verify(log, times(0)).info(Mockito.eq("Tagging container image d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f as registry.example.com/sample:1.0.0"));

        verify(podmanExecutorService, times(1)).tag(eq("7f55eab001a"), eq("registry.example.com/image-name-number-1:0.2.1"));
        verify(podmanExecutorService, times(1)).tag(eq("d2efc6645cbb6ea012f8adcaaab6b03ef847dd3d2b4fa418ca4cde57eff28a7f"), eq("registry.example.com/image-name-number-2:0.2.1"));

        verify(log, times(1)).info(Mockito.eq("Built container image."));
    }

    private void configureMojo(PodmanConfiguration podman, ImageConfiguration image, boolean skipAuth, boolean skipAll, boolean skipBuild, boolean skipTag, boolean failOnMissingContainerFile) {
        buildMojo.podman = podman;
        buildMojo.skip = skipAll;
        buildMojo.skipBuild = skipBuild;
        buildMojo.skipAuth = skipAuth;
        buildMojo.skipTag = skipTag;
        buildMojo.images = image == null ? null : List.of(image);
        buildMojo.pushRegistry = "registry.example.com";
        buildMojo.failOnMissingContainerfile = failOnMissingContainerFile;
    }
}
