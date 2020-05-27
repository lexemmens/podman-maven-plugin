package nl.lexemmens.podman.executor;

import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.exec.ProcessExecutor;

import java.util.List;

public interface CommandExecutorDelegate {
    List<String> executeCommand(ProcessExecutor processExecutor) throws MojoExecutionException;
}
