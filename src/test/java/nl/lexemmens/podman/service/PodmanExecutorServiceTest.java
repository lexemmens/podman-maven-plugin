package nl.lexemmens.podman.service;

import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.image.TestImageConfigurationBuilder;
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

import java.util.ArrayList;
import java.util.List;

import static nl.lexemmens.podman.enumeration.TlsVerify.FALSE;
import static nl.lexemmens.podman.enumeration.TlsVerify.TRUE;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class PodmanExecutorServiceTest {

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
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, FALSE, delegate);
        podmanExecutorService.login("registry.example.com", "username", "password");

        Assertions.assertEquals("podman login --tls-verify=false registry.example.com -u username -p password", delegate.getCommandAsString());
    }

    @Test
    public void testLoginWithoutTls() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, null, delegate);
        podmanExecutorService.login("registry.example.com", "username", "password");

        Assertions.assertEquals("podman login registry.example.com -u username -p password", delegate.getCommandAsString());
    }

    @Test
    public void testLoginFailed() throws MojoExecutionException {
        podmanExecutorService = new PodmanExecutorService(log, FALSE, commandExecutorDelegate);

        when(commandExecutorDelegate.executeCommand(isA(ProcessExecutor.class))).thenThrow(new MojoExecutionException("Login failed"));

        Assertions.assertThrows(MojoExecutionException.class, () -> podmanExecutorService.login("registry.example.com", "username", "password"));
    }

    @Test
    public void testLoginPasswordObfuscatedUponFailure() throws MojoExecutionException {
        podmanExecutorService = new PodmanExecutorService(log, FALSE, commandExecutorDelegate);

        when(commandExecutorDelegate.executeCommand(isA(ProcessExecutor.class))).thenThrow(new MojoExecutionException("Command failed: podman login --tls-verify=false registry.example.com -u username -p password"));

        try {
            podmanExecutorService.login("registry.example.com", "username", "password");
            Assertions.fail("This should not happen");
        } catch (MojoExecutionException e) {
            Assertions.assertEquals("Command failed: podman login --tls-verify=false registry.example.com -u username -p *****", e.getMessage());
        }
    }

    @Test
    public void testPush() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, TRUE, delegate);
        podmanExecutorService.push("registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman push --tls-verify=true registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testTag() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, TRUE, delegate);
        podmanExecutorService.tag("this_is_an_image_hash", "registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman tag this_is_an_image_hash registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testRemoveLocalImage() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, TRUE, delegate);
        podmanExecutorService.removeLocalImage("registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman rmi registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testSave() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        podmanExecutorService = new PodmanExecutorService(log, TRUE, delegate);
        podmanExecutorService.save("image_arhive.tar.gz", "registry.example.com/sample/1.0.0");

        Assertions.assertEquals("podman save --format=oci-archive --output image_arhive.tar.gz registry.example.com/sample/1.0.0", delegate.getCommandAsString());
    }

    @Test
    public void testBuild() throws MojoExecutionException {
        when(mavenProject.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target");

        ImageConfiguration image = new TestImageConfigurationBuilder("test_image")
                .setDockerfileDir("src/test/resources")
                .initAndValidate(mavenProject, log)
                .build();

        String sampleImageHash = "this_would_normally_be_an_image_hash";
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate(List.of(sampleImageHash));
        podmanExecutorService = new PodmanExecutorService(log, TRUE, delegate);

        podmanExecutorService.build(image);

        Assertions.assertEquals("podman build --tls-verify=true --file=" + image.getBuild().getTargetDockerfile() + " --no-cache=false .",
                delegate.getCommandAsString());
    }


    private static class InterceptorCommandExecutorDelegate implements CommandExecutorDelegate {

        private final List<String> processOutput;
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
            return processOutput;
        }

        String getCommandAsString() {
            return StringUtils.join(executedCommands, " ");
        }
    }
}
