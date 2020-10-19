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
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class MultiStageBuildMojoTest extends AbstractMojoTest {

    @InjectMocks
    private BuildMojo buildMojo;

    private ContainerfileDecorator containerfileDecorator;

    @Before
    public void setup() {
        initMocks(this);

        containerfileDecorator = new ContainerfileDecorator(log, mavenFileFilter, mavenProject);
    }

    @Test
    public void testMultiStageBuildTagOnlyFinalImage() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = MultiStageBuildMojoTest.class.getResource("/samplebuildoutput.txt").toURI();
        Path sampleBuildOutputPath = Path.of(sampleBuildOutputUri);

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
        configureMojo(podman, image, true, false, false, false);

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
        verify(log, times(1)).info(Mockito.eq("Built container image."));
    }

    @Test
    public void testMultiStageBuildWithCustomTagPerStage() throws MojoExecutionException, IOException, URISyntaxException {
        URI sampleBuildOutputUri = MultiStageBuildMojoTest.class.getResource("/samplebuildoutput.txt").toURI();
        Path sampleBuildOutputPath = Path.of(sampleBuildOutputUri);

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
        configureMojo(podman, image, true, false, false, false);

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
        verify(log, times(1)).info(Mockito.eq("Built container image."));
    }

    private void configureMojo(PodmanConfiguration podman, ImageConfiguration image, boolean skipAuth, boolean skipAll, boolean skipBuild, boolean skipTag) {
        buildMojo.podman = podman;
        buildMojo.skip = skipAll;
        buildMojo.skipBuild = skipBuild;
        buildMojo.skipAuth = skipAuth;
        buildMojo.skipTag = skipTag;
        buildMojo.images = image == null ? null : List.of(image);
        buildMojo.pushRegistry = "registry.example.com";
    }
}
