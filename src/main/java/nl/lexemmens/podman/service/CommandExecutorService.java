package nl.lexemmens.podman.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Helper class to execute command line processes
 */
public class CommandExecutorService {

    private final Log log;

    /**
     * Constructor
     *
     * @param log The logger
     */
    public CommandExecutorService(Log log) {
        this.log = log;
    }

    /**
     * Executes a command in the supplied basedir and returns its output as a {@link List} of {@link String}s. The process
     * is expected to terminate normally (i.e. exit code 0).
     * <p/>
     * Throws a MojoExecutionException in case an exception is thrown (i.e. non-zero exit code).
     *
     * @param baseDir The directory from which the command should be executed
     * @param command The command to execute as a var-args aray
     * @return The process output as a list of Strings
     * @throws MojoExecutionException In case the process abnormally terminates
     */
    public List<String> runCommand(File baseDir, String... command) throws MojoExecutionException {
        try {
            String msg = String.format("Executing command %s from basedir %s", StringUtils.join(command, " "), baseDir);
            log.debug(msg);
            ProcessResult process = new ProcessExecutor()
                    .directory(baseDir)
                    .command(command)
                    .readOutput(true)
                    .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                    .redirectError(Slf4jStream.of(getClass().getSimpleName()).asError())
                    .exitValueNormal()
                    .execute();

            return process.getOutput().getLinesAsUTF8();
        } catch (IOException | InterruptedException | TimeoutException e) {
            String msg = String.format("Failed to execute command %s - caught %s", StringUtils.join(command, " "), e.getMessage());
            log.error(msg);
            throw new MojoExecutionException(msg, e);
        }
    }

}
