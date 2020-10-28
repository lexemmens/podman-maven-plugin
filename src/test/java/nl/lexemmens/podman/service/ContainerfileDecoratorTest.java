package nl.lexemmens.podman.service;

import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.image.TestImageConfigurationBuilder;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class ContainerfileDecoratorTest {

    @Mock
    private Log log;

    @Mock
    private MavenFileFilter mavenFileFilter;

    @Mock
    private MavenProject mavenProject;

    @Mock
    private Build build;


    private ContainerfileDecorator containerfileDecorator;

    @Before
    public void setup() {
        initMocks(this);

        containerfileDecorator = new ContainerfileDecorator(log, mavenFileFilter, mavenProject);
    }

    @Test
    public void testFailedFilteringThrowsExcepton() throws MavenFilteringException, MojoExecutionException {
        doThrow(new MavenFilteringException("Some exception message!")).when(mavenFileFilter).copyFile(isA(MavenFileFilterRequest.class));
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        ImageConfiguration image = new TestImageConfigurationBuilder("test")
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();
        Assertions.assertThrows(MojoExecutionException.class, () -> containerfileDecorator.decorateContainerfile(image));

        verify(log, Mockito.times(1)).debug(Mockito.eq("Filtering Containerfile. Source: " + image.getBuild().getSourceContainerFileDir() + ", target: " + image.getBuild().getTargetContainerFile()));
        verify(log, Mockito.times(1)).error(Mockito.eq("Failed to filter Containerfile! Some exception message!"), isA(MavenFilteringException.class));
    }

    @Test
    public void testDecorationWithNoLabels() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        ImageConfiguration image = new TestImageConfigurationBuilder("test")
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();
        Assertions.assertDoesNotThrow(() -> containerfileDecorator.decorateContainerfile(image));

        verify(log, Mockito.times(1)).debug(Mockito.eq("No labels to add to the Containerfile"));
        verify(log, Mockito.times(1)).debug(Mockito.eq("Filtering Containerfile. Source: " + image.getBuild().getSourceContainerFileDir() + ", target: " + image.getBuild().getTargetContainerFile()));
    }

    @Test
    public void testDecorationWithLabels() throws IOException, MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/podman-test");

        Files.createDirectories(Paths.get("target/podman-test"));
        Files.deleteIfExists(Paths.get("target/podman-test/Containerfile"));
        Files.copy(Paths.get("src/test/resources/Containerfile"), Paths.get("target/podman-test/Containerfile"));

        ImageConfiguration image = new TestImageConfigurationBuilder("test")
                .setContainerfileDir("src/test/resources")
                .setLabels(Map.of("testLabelKey", "testLabelValue"))
                .initAndValidate(mavenProject, log, true)
                .build();
        Assertions.assertDoesNotThrow(() -> containerfileDecorator.decorateContainerfile(image));

        verify(log, Mockito.times(1)).debug(Mockito.eq("Filtering Containerfile. Source: " + image.getBuild().getSourceContainerFileDir() + ", target: " + image.getBuild().getTargetContainerFile()));

        List<String> collect = Files.lines(Paths.get("target/podman-test/Containerfile")).collect(Collectors.toList());
        String lastLine = collect.get(1);

        Assertions.assertEquals("LABEL testLabelKey=testLabelValue ", lastLine);
    }

    @Test
    public void testDecorationWithLabelsNoTargetFile() throws IOException, MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        Files.createDirectories(Paths.get("target/podman-test"));
        Files.deleteIfExists(Paths.get("target/podman-test/Containerfile"));
        Files.copy(Paths.get("src/test/resources/Containerfile"), Paths.get("target/podman-test/Containerfile"));

        ImageConfiguration image = new TestImageConfigurationBuilder("test")
                .setContainerfileDir("src/test/resources")
                .setLabels(Map.of("testLabelKey", "testLabelValue"))
                .initAndValidate(mavenProject, log, true)
                .build();
        Assertions.assertThrows(MojoExecutionException.class, () -> containerfileDecorator.decorateContainerfile(image));

        verify(log, Mockito.times(1)).debug(eq("Filtering Containerfile. Source: " + image.getBuild().getSourceContainerFileDir() + ", target: " + image.getBuild().getTargetContainerFile()));
        verify(log, Mockito.times(1)).error(eq("Failed to add labels (LABEL testLabelKey=testLabelValue ) to Containerfile: " + image.getBuild().getTargetContainerFile()), isA(Exception.class));

    }
}
