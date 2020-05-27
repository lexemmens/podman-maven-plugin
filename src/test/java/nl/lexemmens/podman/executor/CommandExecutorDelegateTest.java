package nl.lexemmens.podman.executor;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.util.List;

public class CommandExecutorDelegateTest {

    @Test
    public void testSuccesfullCommand() throws MojoExecutionException {
        CommandExecutorDelegate delegate = new CommandExecutorDelegateImpl();

        ProcessExecutor pe = new ProcessExecutor()
                .directory(new File("."))
                .command("echo", "hello", "world")
                .readOutput(true)
                .exitValueNormal();

        List<String> output = delegate.executeCommand(pe);
        Assertions.assertEquals(1, output.size());

        String outputLine = output.get(0);
        Assertions.assertEquals("hello world", outputLine);
    }

    @Test
    public void testFailedCommand() throws MojoExecutionException {
        CommandExecutorDelegate delegate = new CommandExecutorDelegateImpl();

        ProcessExecutor pe = new ProcessExecutor()
                .directory(new File("."))
                .command("unknown_command")
                .readOutput(true)
                .exitValueNormal();

        Assertions.assertThrows(MojoExecutionException.class, () -> delegate.executeCommand(pe));
    }


}
