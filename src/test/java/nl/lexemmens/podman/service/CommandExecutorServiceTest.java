package nl.lexemmens.podman.service;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class CommandExecutorServiceTest {

    @Mock
    private Log log;

    private CommandExecutorService commandExecutorService;

    @Before
    public void setup() {
        initMocks(this);

        commandExecutorService = new CommandExecutorService(log);
    }

    @Test
    public void testHelloWorldCommand() throws MojoExecutionException {
        List<String> result = commandExecutorService.runCommand(new File("."), "echo", "hello", "world");
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(1, result.size());

        String resultString = result.get(0);
        Assertions.assertEquals("hello world", resultString);
    }

}
