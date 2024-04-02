package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.config.skopeo.TestSkopeoConfigurationBuilder;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class SkopeoExecutorServiceTest {
    @Mock
    private Log log;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCopyImage() throws MojoExecutionException {
        SkopeoConfiguration skopeoConfiguration = new TestSkopeoConfigurationBuilder()
                .build();
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        SkopeoExecutorService skopeoExecutorService = new SkopeoExecutorService(log, skopeoConfiguration, delegate);
        skopeoExecutorService.copy("dep.stage.registry.example.com", "dep.release.registry.example.com");
        assertEquals(delegate.getExecutedCommands(), Collections.singletonList(Arrays.asList(
                "skopeo",
                "copy",
                "--src-tls-verify=false",
                "--dest-tls-verify=false",
                "docker://dep.stage.registry.example.com",
                "docker://dep.release.registry.example.com"
        )));
    }

    @Test
    public void testSrcTls() throws MojoExecutionException {
        SkopeoConfiguration skopeoConfiguration = new TestSkopeoConfigurationBuilder()
                .openCopy()
                .setSrcTlsVerify(true)
                .closeCopy()
                .build();
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        SkopeoExecutorService skopeoExecutorService = new SkopeoExecutorService(log, skopeoConfiguration, delegate);
        skopeoExecutorService.copy("dep.stage.registry.example.com", "dep.release.registry.example.com");
        assertEquals(delegate.getExecutedCommands(), Collections.singletonList(Arrays.asList(
                "skopeo",
                "copy",
                "--src-tls-verify=true",
                "--dest-tls-verify=false",
                "docker://dep.stage.registry.example.com",
                "docker://dep.release.registry.example.com"
        )));
    }

    @Test
    public void testDestTls() throws MojoExecutionException {
        SkopeoConfiguration skopeoConfiguration = new TestSkopeoConfigurationBuilder()
                .openCopy()
                .setDestTlsVerify(true)
                .closeCopy()
                .build();
        InterceptorCommandExecutorDelegate delegate = new InterceptorCommandExecutorDelegate();
        SkopeoExecutorService skopeoExecutorService = new SkopeoExecutorService(log, skopeoConfiguration, delegate);
        skopeoExecutorService.copy("dep.stage.registry.example.com", "dep.release.registry.example.com");
        assertEquals(delegate.getExecutedCommands(), Collections.singletonList(Arrays.asList(
                "skopeo",
                "copy",
                "--src-tls-verify=false",
                "--dest-tls-verify=true",
                "docker://dep.stage.registry.example.com",
                "docker://dep.release.registry.example.com"
        )));
    }


    private static class InterceptorCommandExecutorDelegate implements CommandExecutorDelegate {

        private final List<String> processOutput;
        private final List<List<String>> executedCommands = new ArrayList<>();

        InterceptorCommandExecutorDelegate() {
            this.processOutput = new ArrayList<>();
        }

        @Override
        public List<String> executeCommand(ProcessExecutor processExecutor) {
            executedCommands.add(processExecutor.getCommand());
            return processOutput;
        }

        List<List<String>> getExecutedCommands() {
            return executedCommands;
        }
    }
}
