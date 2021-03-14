package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.StageConfiguration;
import nl.lexemmens.podman.config.image.batch.BatchImageConfiguration;
import nl.lexemmens.podman.config.image.batch.TestBatchImageConfigurationBuilder;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.podman.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.enumeration.ContainerFormat;
import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.service.ContainerfileDecorator;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * This class only tests conversion from {@link BatchImageConfiguration} to {@link SingleImageConfiguration}. Building the image itself
 * is already tested in the {@link BuildMojoTest}. If conversion works as expected, then we can be sure that
 * building the container image will be working as expected.
 */
@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BatchBuildMojoTest extends AbstractMojoTest {

    @InjectMocks
    private BuildMojo buildMojo;

    @Mock
    private ContainerfileDecorator containerfileDecorator;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testNoImageNameThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        BatchImageConfiguration batch = new TestBatchImageConfigurationBuilder(null).build();

        configureMojo(podman, batch);

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
        BatchImageConfiguration batch = new TestBatchImageConfigurationBuilder(null).build();
        batch.setCustomImageNameForMultiStageContainerfile(true);

        configureMojo(podman, batch);

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
        BatchImageConfiguration image = new TestBatchImageConfigurationBuilder("sample-image", null).build();

        configureMojo(podman, image);

        try {
            buildMojo.execute();
            fail("Expected an exception, however none was thrown.");
        } catch (Exception e) {
            assertEquals("Missing <build/> section in batch image configuration!", e.getMessage());
        }
    }

    @Test
    public void testBatchWithInvalidContainerfileDirThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        BatchImageConfiguration image = new TestBatchImageConfigurationBuilder("sample-image")
                .setContainerfileDir("src/test/resources/non-existing-dir")
                .build();

        configureMojo(podman, image);

        Build mockBuild = Mockito.mock(Build.class);
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        when(mavenProject.getBuild()).thenReturn(mockBuild);
        when(mockBuild.getDirectory()).thenReturn("target");

        try {
            buildMojo.execute();
            fail("Expected an exception, however none was thrown.");
        } catch (Exception e) {
            assertEquals("Failed to find Containerfiles with name 'Containerfile' in directory src/test/resources/non-existing-dir", e.getMessage());
        }
    }

    @Test
    public void testBatchWithEmptyContainerfileDirThrowsException() {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        BatchImageConfiguration image = new TestBatchImageConfigurationBuilder("sample-image")
                .setContainerfileDir("src/test/resources/emptydir")
                .build();

        configureMojo(podman, image);

        Build mockBuild = Mockito.mock(Build.class);
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        when(mavenProject.getBuild()).thenReturn(mockBuild);
        when(mockBuild.getDirectory()).thenReturn("target");

        try {
            buildMojo.execute();
            fail("Expected an exception, however none was thrown.");
        } catch (Exception e) {
            assertEquals("Invalid batch configuration found!", e.getMessage());
        }
    }

    @Test
    public void testSuccessBatchBuildWithSingleContainerfileNoTags() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        BatchImageConfiguration image = new TestBatchImageConfigurationBuilder("sample-image")
                .setContainerfileDir("src/test/resources/customdockerfile")
                .build();

        configureMojo(podman, image);

        Build mockBuild = Mockito.mock(Build.class);
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        when(mavenProject.getBuild()).thenReturn(mockBuild);
        when(mockBuild.getDirectory()).thenReturn("target");

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        assertDoesNotThrow(() -> buildMojo.execute());
    }

    @Test
    public void testConversionWithCustomValues() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();

        StageConfiguration stageCfg = new StageConfiguration();
        stageCfg.setImageName("custom-image-name-for-stage-%d");
        stageCfg.setName("stage");

        BatchImageConfiguration image = new TestBatchImageConfigurationBuilder("sample-image-%d")
                .setContainerfile("Containerfile")
                .setPull(true)
                .setPullAlways(true)
                .setStages(new StageConfiguration[]{stageCfg})
                .setLabels(Collections.singletonMap("KEY", "VALUE"))
                .setTags(new String[]{"latest"})
                .setFormat(ContainerFormat.DOCKER)
                .build();

        configureMojo(podman, image);

        Build mockBuild = Mockito.mock(Build.class);
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        when(mavenProject.getBuild()).thenReturn(mockBuild);
        when(mavenProject.getBasedir()).thenReturn(new File("src/test/resources/batch/subdir"));
        when(mockBuild.getDirectory()).thenReturn("target");
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        assertDoesNotThrow(() -> buildMojo.execute());
        assertEquals(1, buildMojo.resolvedImages.size());

        SingleImageConfiguration image1 = buildMojo.resolvedImages.get(0);
        assertEquals("sample-image-subdir", image1.getImageName());
        assertEquals(1, image1.getStages().length);
        assertFalse(image1.useCustomImageNameForMultiStageContainerfile());
        assertNotNull(image1.getBuild());
        assertEquals(1, image1.getBuild().getAllTags().size());
        assertEquals("latest", image1.getBuild().getAllTags().get(0));
        assertTrue(image1.getBuild().isPull());
        assertTrue(image1.getBuild().isPullAlways());
        assertEquals(ContainerFormat.DOCKER, image1.getBuild().getFormat());
    }

    @Test
    public void testSuccessBatchBuildWithMultipleContainerfileAndTags() throws MojoExecutionException {
        PodmanConfiguration podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.FALSE).build();
        StageConfiguration phase2Config = new StageConfiguration();
        phase2Config.setImageName("custom-image-name-%d");
        phase2Config.setName("phase2");

        BatchImageConfiguration image = new TestBatchImageConfigurationBuilder("sample-image-%d")
                .setContainerfileDir("src/test/resources/batch")
                .setTags(new String[]{"1.0.0"})
                .setStages(new StageConfiguration[]{phase2Config})
                .build();

        configureMojo(podman, image);

        Build mockBuild = Mockito.mock(Build.class);
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        when(mavenProject.getBuild()).thenReturn(mockBuild);
        when(mockBuild.getDirectory()).thenReturn("target");

        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class))).thenReturn(serviceHub);
        when(serviceHub.getContainerfileDecorator()).thenReturn(containerfileDecorator);
        when(serviceHub.getPodmanExecutorService()).thenReturn(podmanExecutorService);
        when(podmanExecutorService.build(isA(SingleImageConfiguration.class))).thenReturn(Collections.singletonList("ca1f5f48ef431c0818d5e8797dfe707557bdc728fe7c3027c75de18f934a3b76"));

        assertDoesNotThrow(() -> buildMojo.execute());

        assertEquals(2, buildMojo.resolvedImages.size());

        SingleImageConfiguration image1 = buildMojo.resolvedImages.get(0);
        assertEquals("sample-image-batch", image1.getImageName());
        assertEquals(1, image1.getStages().length);
        assertFalse(image1.useCustomImageNameForMultiStageContainerfile());
        assertNotNull(image1.getBuild());
        assertEquals(1, image1.getBuild().getAllTags().size());
        assertEquals("1.0.0", image1.getBuild().getAllTags().get(0));

        SingleImageConfiguration image2 = buildMojo.resolvedImages.get(1);
        assertEquals("sample-image-subdir", image2.getImageName());
        assertEquals(1, image2.getStages().length);
        assertFalse(image2.useCustomImageNameForMultiStageContainerfile());
        assertNotNull(image2.getBuild());
        assertEquals(1, image2.getBuild().getAllTags().size());
        assertEquals("1.0.0", image2.getBuild().getAllTags().get(0));
    }


    private void configureMojo(PodmanConfiguration podman, BatchImageConfiguration batch) {
        buildMojo.podman = podman;
        buildMojo.skip = false;
        buildMojo.skipBuild = false;
        buildMojo.skipAuth = true;
        buildMojo.skipTag = true;
        buildMojo.batch = batch;
        buildMojo.pushRegistry = "registry.example.com";
        buildMojo.failOnMissingContainerfile = true;
    }
}
