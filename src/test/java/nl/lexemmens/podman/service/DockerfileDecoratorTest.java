package nl.lexemmens.podman.service;

import nl.lexemmens.podman.image.ImageConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class DockerfileDecoratorTest {

    @Mock
    private Log log;

    @Mock
    private MavenFileFilter mavenFileFilter;

    @Mock
    private MavenProject mavenProject;


    private DockerfileDecorator dockerfileDecorator;

    @Before
    public void setup() {
        initMocks(this);

        dockerfileDecorator = new DockerfileDecorator(log, mavenFileFilter, mavenProject);
    }

    @Test
    public void testFailedFilteringThrowsExcepton() throws MavenFilteringException {
        doThrow(new MavenFilteringException("Some exception message!")).when(mavenFileFilter).copyFile(isA(MavenFileFilterRequest.class));

        Assertions.assertThrows(MojoExecutionException.class, () -> dockerfileDecorator.decorateDockerfile(getImageConfiguration()));

        verify(log, Mockito.times(1)).debug(Mockito.eq("Filtering Dockerfile. Source: Dockerfile, target: target/Dockerfile"));
        verify(log, Mockito.times(1)).error(Mockito.eq("Failed to filter Dockerfile! Some exception message!"), isA(MavenFilteringException.class));
    }

    private ImageConfiguration getImageConfiguration() {
        ImageConfiguration imageConfiguration = new ImageConfiguration();
//        imageConfiguration.
//
//                .setSourceDockerFile(Paths.get("Dockerfile"))
//                .setTargetDockerfile(Paths.get("target/Dockerfile"))
//                .build();

    }

}
