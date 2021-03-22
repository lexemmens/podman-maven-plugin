package nl.lexemmens.podman.executor;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.util.List;

/**
 * Delegate class that is responsible for actually executing a specific command. Putting this in
 * a separate class allows for better testing.
 */
public final class CommandExecutorDelegateImpl implements CommandExecutorDelegate {

    @Override
    public List<String> executeCommand(ProcessExecutor processExecutor) throws MojoExecutionException {
        try {
            ProcessResult process = processExecutor.execute();
            return process.getOutput().getLinesAsUTF8();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            String msg = String.format("Failed to execute command '%s' - caught %s", StringUtils.join(processExecutor.getCommand(), " "), e.getMessage());
            throw new MojoExecutionException(msg);
        }
    }

}
