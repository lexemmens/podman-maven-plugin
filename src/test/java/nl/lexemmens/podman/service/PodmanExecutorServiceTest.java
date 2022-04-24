package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.image.single.TestSingleImageConfigurationBuilder;
import nl.lexemmens.podman.config.podman.TestPodmanConfigurationBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.PumpStreamHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.lexemmens.podman.enumeration.ContainerFormat.DOCKER;
import static nl.lexemmens.podman.enumeration.ContainerFormat.OCI;
import static nl.lexemmens.podman.enumeration.TlsVerify.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PodmanExecutorServiceTest {

    public static final String PASSWORD = "secretPassword";
    public static final String USERNAME = "username";
    @Mock
    private MavenProject mavenProject;

    @Mock
    private Build build;

    @Mock
    private Log log;

    @Mock
    private CommandExecutorDelegate commandExecutorDelegate;

    private PodmanExecutorService podmanExecutorService;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testLogin() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(FALSE).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.login("registry.example.com", USERNAME, PASSWORD);

        Assertions.assertEquals("podman login --tls-verify=false registry.example.com --username=username --password-stdin", delegate.getCommandAsString());
        validatePasswordProcessInput(PASSWORD, delegate);
    }

    @Test
    public void testLoginWithTlsNotSpecified() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(NOT_SPECIFIED).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.login("registry.example.com", USERNAME, PASSWORD);

        Assertions.assertEquals("podman login registry.example.com --username=username --password-stdin", delegate.getCommandAsString());
        validatePasswordProcessInput(PASSWORD, delegate);
    }

    private void validatePasswordProcessInput(String expectedPassword, InterceptorCommandExecutorDelegate delegate) {
        Assertions.assertEquals(1, delegate.processInput.size());
        Assertions.assertEquals(expectedPassword, delegate.processInput.get(0));
    }

    @Test
    public void testLoginWithoutTls() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.login("registry.example.com", USERNAME, PASSWORD);

        Assertions.assertEquals("podman login registry.example.com --username=username --password-stdin", delegate.getCommandAsString());
        validatePasswordProcessInput(PASSWORD, delegate);
    }

    @Test
    public void testLoginFailed() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(FALSE).initAndValidate(mavenProject, log).build();

        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, commandExecutorDelegate);

        when(commandExecutorDelegate.executeCommand(isA(ProcessExecutor.class))).thenThrow(new MojoExecutionException("Login failed"));

        Assertions.assertThrows(MojoExecutionException.class, () -> podmanExecutorService.login("registry.example.com", USERNAME, PASSWORD));
    }

    @Test
    public void testLoginPasswordObfuscatedUponFailure() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(FALSE).initAndValidate(mavenProject, log).build();

        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, commandExecutorDelegate);

        when(commandExecutorDelegate.executeCommand(isA(ProcessExecutor.class))).thenThrow(new MojoExecutionException("Command failed: podman login --tls-verify=false registry.example.com --username=username --password-stdin"));

        try {
            podmanExecutorService.login("registry.example.com", USERNAME, PASSWORD);
            Assertions.fail("This should not happen");
        } catch (MojoExecutionException e) {
            Assertions.assertFalse(e.getMessage().contains(PASSWORD));
            Assertions.assertEquals("Command failed: podman login --tls-verify=false registry.example.com --username=username --password-stdin", e.getMessage());
        }
    }

    @Test
    public void testPush() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.push("registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman push --tls-verify=true registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testVersion() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.version();

        Assertions.assertEquals("podman version", delegate.getCommandAsString());
    }

    @Test
    public void testTag() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.tag("this_is_an_image_hash", "registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman tag this_is_an_image_hash registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testRemoveLocalImage() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.removeLocalImage("registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman rmi registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testSave() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);
        podmanExecutorService.save("image_arhive.tar.gz", "registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman save --format=oci-archive --output image_arhive.tar.gz registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testBuildOciFormat() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --format=oci --file=" + image.getBuild().getTargetContainerFile() + " --no-cache=false .",
                delegate.getCommandAsString());
    }

    @Test
    public void testBuildDockerFormat() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setFormat(DOCKER)
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --format=docker --file=" + image.getBuild().getTargetContainerFile() + " --no-cache=false .",
                delegate.getCommandAsString());
    }


    @Test
    public void testBuildNoLayers() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setFormat(OCI)
                .setLayers(false)
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --layers=false --format=oci --file="
                + image.getBuild().getTargetContainerFile() + " --no-cache=false .", delegate.getCommandAsString());
    }

    @Test
    public void testBuildPullAlways() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setFormat(OCI)
                .setPullAlways(true)
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --format=oci --file=" + image.getBuild().getTargetContainerFile() + " --no-cache=false " +
                "--pull-always=true .", delegate.getCommandAsString());
    }

    @Test
    public void testBuildPull() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setFormat(OCI)
                .setPull(true)
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --format=oci --file=" + image.getBuild().getTargetContainerFile() + " --no-cache=false " +
                "--pull=true .", delegate.getCommandAsString());
    }

    @Test
    public void testBuildSquash() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setFormat(OCI)
                .setSquash(true)
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --squash --format=oci --file="
                + image.getBuild().getTargetContainerFile() + " --no-cache=false .", delegate.getCommandAsString());
    }

    @Test
    public void testBuildSquashAll() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(TRUE).initAndValidate(mavenProject, log).build();
        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setFormat(OCI)
                .setSquashAll(true)
                .setContainerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --squash-all --format=oci --file="
                + image.getBuild().getTargetContainerFile() + " --no-cache=false .", delegate.getCommandAsString());
    }

    @Test
    public void testBuildWithCustomRootDir() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder()
                .setTlsVerify(TRUE)
                .setRoot(new File("/some/custom/root/dir"))
                .initAndValidate(mavenProject, log)
                .build();

        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setContainerfileDir("src/test/resources")
                .setFormat(OCI)
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman --root=/some/custom/root/dir build --tls-verify=true --format=oci --file=" + image.getBuild().getTargetContainerFile() +
                        " --no-cache=false .", delegate.getCommandAsString());
    }

    @Test
    public void testBuildWithCustomRunRootDir() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder()
                .setTlsVerify(TRUE)
                .setRunRoot(new File("/some/custom/runroot/dir"))
                .initAndValidate(mavenProject, log)
                .build();

        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setContainerfileDir("src/test/resources")
                .setFormat(OCI)
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman --runroot=/some/custom/runroot/dir build --tls-verify=true --format=oci --file=" + image.getBuild().getTargetContainerFile() +
                        " --no-cache=false .", delegate.getCommandAsString());
    }

    @Test
    public void testBuildWithCustomRootAndRunRootDir() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder()
                .setTlsVerify(TRUE)
                .setRoot(new File("/some/custom/root/dir"))
                .setRunRoot(new File("/some/custom/runroot/dir"))
                .initAndValidate(mavenProject, log)
                .build();

        SingleImageConfiguration image = new TestSingleImageConfigurationBuilder("test_image")
                .setContainerfileDir("src/test/resources")
                .setFormat(OCI)
                .initAndValidate(mavenProject, log, true)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(Collections.singletonList(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, podmanConfig, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman --root=/some/custom/root/dir --runroot=/some/custom/runroot/dir build --tls-verify=true --format=oci " +
                        "--file=" + image.getBuild().getTargetContainerFile() + " --no-cache=false .",
                delegate.getCommandAsString());
    }


    private static class InterceptorCommandExecutorDelegate implements CommandExecutorDelegate {

        private final List<String> processOutput;
        private List<String> processInput;
        private List<String> executedCommands;

        InterceptorCommandExecutorDelegate() {
            this.processOutput = new ArrayList<>();
        }

        InterceptorCommandExecutorDelegate(List<String> processOutput) {
            this.processOutput = processOutput;
        }

        @Override
        public List<String> executeCommand(ProcessExecutor processExecutor) {
            executedCommands = processExecutor.getCommand();
            if (processExecutor.streams() instanceof PumpStreamHandler) {
                PumpStreamHandler pumpStreamHandler = (PumpStreamHandler) processExecutor.streams();
                processInput = Optional.ofNullable(pumpStreamHandler.getInput())
                        .map(inputStream -> new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines())
                        .orElse(Stream.empty())
                        .collect(Collectors.toList());
            } else {
                 processInput = new ArrayList<>();
            }
            return processOutput;
        }

        String getCommandAsString() {
            return StringUtils.join(executedCommands, " ");
        }
    }
}
