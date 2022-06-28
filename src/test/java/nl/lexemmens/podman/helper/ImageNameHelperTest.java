package nl.lexemmens.podman.helper;

import nl.lexemmens.podman.config.image.single.SingleImageBuildConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class ImageNameHelperTest {

    @Mock
    private MavenProject mavenProject;

    private ImageNameHelper imageNameHelper;

    @Before
    public void before() {
        imageNameHelper = new ImageNameHelper(mavenProject);
    }

    @Test
    public void testArtifactIdReplacement() {
        when(mavenProject.getArtifactId()).thenReturn("my-Artifact");

        SingleImageConfiguration image = new SingleImageConfiguration();
        image.setImageName("sample-image-%a");

        imageNameHelper.formatImageName(image);

        assertEquals("sample-image-my-artifact", image.getImageName());
    }

    @Test
    public void testContainerFileDirectoryReplacement() {
        SingleImageBuildConfiguration build = new SingleImageBuildConfiguration();
        build.setContainerFile("Containerfile");
        build.setContainerFileDir(new File("/some/directory/hello/world"));

        SingleImageConfiguration image = new SingleImageConfiguration();
        image.setBuild(build);
        image.setImageName("sample-image-hello-%d");

        imageNameHelper.adaptReplacemeents(image);
        imageNameHelper.formatImageName(image);

        assertEquals("sample-image-hello-world", image.getImageName());
    }

    @Test
    public void testGroupIdReplacement() {
        when(mavenProject.getGroupId()).thenReturn("com.example.world.hello");

        SingleImageConfiguration image = new SingleImageConfiguration();
        image.setImageName("sample-image-%g-world");

        imageNameHelper.formatImageName(image);

        assertEquals("sample-image-hello-world", image.getImageName());
    }

    @Test
    public void testImageNumberReplacement() {
        SingleImageConfiguration image = new SingleImageConfiguration();
        image.setImageName("sample-image-%n");

        imageNameHelper.formatImageName(image);
        assertEquals("sample-image-0", image.getImageName());

        SingleImageConfiguration image1 = new SingleImageConfiguration();
        image1.setImageName("sample-image-%n");

        imageNameHelper.formatImageName(image1);
        assertEquals("sample-image-1", image1.getImageName());
    }

    @Test
    public void testSnapshotLatestReplcement() {
        when(mavenProject.getVersion())
                .thenReturn("1.0.0-SNAPSHOT")
                .thenReturn("2.3.0");

        SingleImageConfiguration image = new SingleImageConfiguration();
        image.setImageName("sample-image:%l");
        imageNameHelper.formatImageName(image);
        assertEquals("sample-image:latest", image.getImageName());

        SingleImageConfiguration image1 = new SingleImageConfiguration();
        image1.setImageName("another-sample-image:%l");
        imageNameHelper.formatImageName(image1);
        assertEquals("another-sample-image:2.3.0", image1.getImageName());
    }

    @Test
    public void testSnapshotTimestampReplacement() {
        SingleImageConfiguration image = new SingleImageConfiguration();
        image.setImageName("sample-image:%t");
        imageNameHelper.formatImageName(image);
        assertNotNull(image.getImageName());


        // TODO Make test a little bit more extensive...
    }

    @Test
    public void testProjectVersionReplacement() {
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");

        SingleImageConfiguration image = new SingleImageConfiguration();
        image.setImageName("sample-image:%v");
        imageNameHelper.formatImageName(image);
        assertEquals("sample-image:1.0.0-snapshot", image.getImageName());
    }

}
