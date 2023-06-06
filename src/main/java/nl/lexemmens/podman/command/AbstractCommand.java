package nl.lexemmens.podman.command;

import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.List;

/**
 * Abstract implementation for each command
 */
public abstract class AbstractCommand implements Command {

    private static final File BASE_DIR = new File(".");

    protected final Log log;

    private final CommandExecutorDelegate delegate;
    private final File workDir;


    /**
     * Constructor. Creates a new instance of the command using the default working directory
     */
    protected AbstractCommand(Log log, CommandExecutorDelegate delegate) {
        this(log, delegate, null);
    }

    /**
     * Constructor. Creates a new instance of the command using the specified working directory
     */
    protected AbstractCommand(Log log, CommandExecutorDelegate delegate, File workDir) {
        this.log = log;
        this.delegate = delegate;
        if (workDir == null) {
            this.workDir = BASE_DIR;
        } else {
            this.workDir = workDir;
        }
    }

    @Override
    public List<String> execute() throws MojoExecutionException {
        String msg = String.format("Executing command '%s' from basedir %s", StringUtils.join(getCommand(), " "), BASE_DIR.getAbsolutePath());
        log.debug(msg);
        ProcessExecutor processExecutor = new ProcessExecutor()
                .directory(workDir)
                .command(getCommand())
                .readOutput(true)
                .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                .redirectError(Slf4jStream.of(getClass().getSimpleName()).asError())
                .exitValueNormal();


        // Some processes print regular text on stderror, so make redirecting the error stream configurable.
        if (redirectError()) {
            processExecutor.redirectError(Slf4jStream.of(getClass().getSimpleName()).asError());
        }

        return delegate.executeCommand(processExecutor);
    }

    /**
     * Returns the command to execute
     *
     * @return The command to execute
     */
    protected abstract List<String> getCommand();

    /**
     * Returns whether errors should be redirected
     */
    protected abstract boolean redirectError();
}
