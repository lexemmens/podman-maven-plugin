package nl.lexemmens.podman.executor;

import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.exec.ProcessExecutor;

import java.util.List;

/**
 * Interface that allows delegation of command execution to a specific implementation
 */
public interface CommandExecutorDelegate {

    /**
     * <p>
     * Executes the command as configured in the provided ProcessExecutor.
     * </p>
     * <p>
     * Throws a MojoExecutionException in case command execution fails
     * </p>
     *
     * @param processExecutor The process executor carrying the command to execute
     * @return A {@link List} of {@link String}s representing the actual output of the command
     * @throws MojoExecutionException In case execution of the command fails
     */
    List<String> executeCommand(ProcessExecutor processExecutor) throws MojoExecutionException;
}
