package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.podman.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static nl.lexemmens.podman.enumeration.TlsVerify.FALSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BuildahExecutorServiceTest {

    @Mock
    private Log log;

    @Mock
    private MavenProject mavenProject;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCleanupLocalContainerStorageWithNoCustomRoot() throws MojoExecutionException {
        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder().setTlsVerify(FALSE).initAndValidate(mavenProject, log).build();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        BuildahExecutorService buildahExecutorService = new BuildahExecutorService(log, podmanConfig, delegate);
        buildahExecutorService.cleanupLocalContainerStorage();

        verify(log, Mockito.times(1)).info("Podman root storage location is set to its defaults. Not cleaning up this storage location.");
        assertNull(delegate.getCommandAsString());
    }

    @Test
    public void testCleanupLocalContainerStorageWithCustomRoot() throws MojoExecutionException {
        File customRoot = new File("");

        PodmanConfiguration podmanConfig = new TestPodmanConfigurationBuilder()
                .setTlsVerify(FALSE)
                .setRoot(customRoot)
                .initAndValidate(mavenProject, log).build();

        String expectedCommand = "buildah unshare rm -rf " + customRoot.getAbsolutePath();

        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        BuildahExecutorService buildahExecutorService = new BuildahExecutorService(log, podmanConfig, delegate);
        buildahExecutorService.cleanupLocalContainerStorage();

        verify(log, Mockito.times(0)).info("Podman root storage location is set to its defaults. Not cleaning up this storage location.");
        assertEquals(expectedCommand, delegate.getCommandAsString());
    }

    private static class InterceptorCommandExecutorDelegate implements CommandExecutorDelegate {

        private final List<String> processOutput;
        private List<String> executedCommands;

        InterceptorCommandExecutorDelegate() {
            this.processOutput = new ArrayList<>();
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
