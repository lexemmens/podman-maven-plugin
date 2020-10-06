package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.image.PodmanConfiguration;
import nl.lexemmens.podman.image.TestImageConfigurationBuilder;
import nl.lexemmens.podman.image.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.service.DockerfileDecorator;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BuildMojoTest extends AbstractMojoTest {

    @InjectMocks
    private BuildMojo buildMojo;

    private DockerfileDecorator dockerfileDecorator;

    @Before
    public void setup() {
        initMocks(this);

        dockerfileDecorator = new DockerfileDecorator(log, mavenFileFilter, mavenProject);
    }

    @Test
    public void testSkipAllActions() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .build();
        configureMojo(podman, image, false, true, false, false);

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Podman actions are skipped."));
    }

    @Test
    public void testSkipBuild() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .build();
        configureMojo(podman, image, false, false, true, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        buildMojo.execute();

        verify(log, Mockito.times(1)).info(Mockito.eq("Building container images is skipped."));
    }

    @Test
    public void testBuildWithoutConfigThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        configureMojo(podman, null, true, false, false, true);

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildWithEmptyConfigsThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        configureMojo(podman, null, true, false, false, true);

        buildMojo.images = new ArrayList<>();

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildWithoutTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .build();
        configureMojo(podman, image, true, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getDockerfileDecorator()).thenReturn(dockerfileDecorator);
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
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .build();
        configureMojo(null, image, true, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getDockerfileDecorator()).thenReturn(dockerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(ImageConfiguration.class))).thenReturn(List.of("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        Assertions.assertDoesNotThrow(() -> buildMojo.execute());

        verify(log, Mockito.times(1)).info(Mockito.eq("Tagging container images is skipped."));
        verify(mavenFileFilter, Mockito.times(1)).copyFile(isA(MavenFileFilterRequest.class));
        verify(podmanExecutorService, times(1)).build(isA(ImageConfiguration.class));
    }

    @Test
    public void testBuildCustomDockerfileWithoutTag() throws MojoExecutionException, MavenFilteringException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setDockerfile("Containerfile")
                .setDockerfileDir("src/test/resources/customdockerfile")
                .build();
        configureMojo(podman, image, true, false, false, true);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getDockerfileDecorator()).thenReturn(dockerfileDecorator);
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
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .build();
        configureMojo(podman, image, true, false, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getDockerfileDecorator()).thenReturn(dockerfileDecorator);
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
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .setTags(new String[]{})
                .build();
        configureMojo(podman, image, true, false, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getDockerfileDecorator()).thenReturn(dockerfileDecorator);
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
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false);

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        String expectedFullImageName = "registry.example.com/sample:1.0.0";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getDockerfileDecorator()).thenReturn(dockerfileDecorator);
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
                .setDockerfileDir(DEFAULT_DOCKERFILE_DIR)
                .setTags(new String[]{})
                .setCreateLatestTag(true)
                .build();
        configureMojo(podman, image, true, false, false, false);

        String imageHash = "ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76";
        String expectedFullImageName = "registry.example.com/sample:latest";

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getDockerfileDecorator()).thenReturn(dockerfileDecorator);
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
    public void testBuildContextNoDockerfile() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setDockerfileDir("src/test")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildContextWithDefaultDockerfileDir() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(mavenProject.getBasedir()).thenReturn(new File("."));
        when(build.getDirectory()).thenReturn("target");

        // No Containerfile present at root level
        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
    }

    @Test
    public void testBuildContextWithEmptyDockerfile() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        ImageConfiguration image = new TestImageConfigurationBuilder("sample")
                .setDockerfileDir("src/test/resources/emptydockerfile")
                .setTags(new String[]{"1.0.0"})
                .setCreateLatestTag(false)
                .build();
        configureMojo(podman, image, true, false, false, false);

        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Assertions.assertThrows(MojoExecutionException.class, buildMojo::execute);
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
