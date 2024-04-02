package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.podman.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class SecurityContextServiceTest {
    @Mock
    private Log log;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSetSecurityContextSELinuxDisabled() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate("disabled");
        PodmanConfiguration podmanCfg = new TestPodmanConfigurationBuilder().build();

        SecurityContextService securityContextService = new SecurityContextService(log, podmanCfg, delegate);
        securityContextService.setSecurityContext();

        verify(log, times(1)).debug("Checking SELinux status...");
        verify(log, times(1)).debug("Not setting security context because SELinux is disabled.");
    }

    @Test
    public void testSetSecurityContextSELinuxEnabledNoCustomRoot() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate("enabled");
        PodmanConfiguration podmanCfg = new TestPodmanConfigurationBuilder().build();

        SecurityContextService securityContextService = new SecurityContextService(log, podmanCfg, delegate);
        securityContextService.setSecurityContext();

        verify(log, times(1)).debug("Checking SELinux status...");
        verify(log, times(1)).debug("SELinux is enabled");
        verify(log, times(1)).debug("Using Podman default storage location. Assuming security context is set correctly " +
                "for this location. Refer to the documentation of this plugin if you run into any issues.");
    }

    @Test
    public void testSetSecurityContextSELinuxEnabledCustomRoot() throws MojoExecutionException {
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate("enabled");
        PodmanConfiguration podmanCfg = new TestPodmanConfigurationBuilder().setRoot(new File("/tmp/.containers")).build();

        SecurityContextService securityContextService = new SecurityContextService(log, podmanCfg, delegate);
        securityContextService.setSecurityContext();

        List<String> commandExecutions = delegate.getExecutedCommands()
                .stream()
                .map(commands -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    commands.forEach(cmd -> stringBuilder.append(cmd).append(" "));
                    return stringBuilder.toString();
                })
                .filter(command -> command.equals("chcon -R --reference /var/lib/containers/storage /tmp/.containers "))
                .collect(Collectors.toList());

        assertEquals(1, commandExecutions.size());
        verify(log, times(1)).debug("Checking SELinux status...");
        verify(log, times(1)).debug("SELinux is enabled");
        verify(log, times(1)).info("Determined graphRoot location to be: /var/lib/containers/storage. Executing chcon using this directory as reference...");
        verify(log, times(0)).debug("Using Podman default storage location. Assuming security context is set correctly " +
                "for this location. Refer to the documentation of this plugin if you run into any issues.");
    }

    private static class InterceptorCommandExecutorDelegate implements CommandExecutorDelegate {

        private final String seLinuxStatus;
        private final List<String> processOutput;
        private final List<List<String>> executedCommands = new ArrayList<>();

        InterceptorCommandExecutorDelegate(String seLinuxStatus) {
            this.seLinuxStatus = seLinuxStatus;
            this.processOutput = new ArrayList<>();
        }

        @Override
        public List<String> executeCommand(ProcessExecutor processExecutor) {
            executedCommands.add(processExecutor.getCommand());
            if(processExecutor.getCommand().contains("sestatus")) {
                List<String> sestatusOutput = new ArrayList<>();
                sestatusOutput.add(String.format("SELinux status: %s", seLinuxStatus));
                return sestatusOutput;
            }

            if(processExecutor.getCommand().contains("podman")) {
                List<String> sestatusOutput = new ArrayList<>();
                sestatusOutput.add("graphRoot: /var/lib/containers/storage");
                return sestatusOutput;
            }
            return processOutput;
        }

        List<List<String>> getExecutedCommands() {
            return executedCommands;
        }

    }
}
