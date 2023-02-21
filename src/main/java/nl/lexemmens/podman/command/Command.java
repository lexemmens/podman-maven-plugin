package nl.lexemmens.podman.command;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * Generic Command interface
 */
public interface Command {

    /**
     * Executes the command
     *
     * @return A list of Strings representing the output lines
     * @throws MojoExecutionException If the command execution fails
     */
    List<String> execute() throws MojoExecutionException;

}
