package nl.lexemmens.podman.service;

import nl.lexemmens.podman.enumeration.Command;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     Abstract base class to assist realization of a concrete command executor service for commands to be executed.
 * </p>
 *
 * <p>
 *     This class facilitates the fluent configuration of commands, compiling them to full command lines and delegates
 *     them to a configured {@link CommandExecutorDelegate} for execution.
 * </p>
 *
 * @param <T> The Type of commands to be handled by this class.
 */
public abstract class AbstractExecutorService<T extends Command> {
    private static final File BASE_DIR = new File(".");

    protected final CommandExecutorDelegate delegate;
    protected final Log log;

    public AbstractExecutorService(Log log, CommandExecutorDelegate delegate) {
        this.delegate = delegate;
        this.log = log;
    }

    /**
     * Initiate a command execution by providing the main command to be executed.
     *
     * @param command The main command to be executed.
     * @return A {@link CommandExecutionBuilder} for fluent command configuration.
     */
    public CommandExecutionBuilder command(T command) {
        return new CommandExecutionBuilder(command);
    }

    /**
     * <p>
     *     Compile the full command line from the provided main command and sub commands.
     * </p>
     * <p>
     *     A concrete subclass may further decorate the command line based on static configuration or the type of command
     * </p>
     * @param command The main command to be executed.
     * @param subCommands The configured subCommand to be executed.
     * @return The compiled full command line.
     */
    protected abstract List<String> compileCommandLine(T command, List<String> subCommands);

    /**
     * This builder class allows for fluent definition and execution of complex commands.
     */
    public class CommandExecutionBuilder {

        private final T command;
        private final List<String> subCommands = new ArrayList<>();
        private File workDir = BASE_DIR;
        // As most use-cases demand to redirect the error stream, default is true
        private boolean redirectError = true;
        private InputStream inputStream;

        private CommandExecutionBuilder(T command) {
            this.command = command;
        }

        /**
         * Add a subcommand (argument) to the command to be executed.
         *
         * @param subcommand the new subcommand to add
         * @return This builder for further fluent configuration.
         */
        public CommandExecutionBuilder subCommand(String subcommand) {
            subCommands.add(subcommand);
            return this;
        }

        /**
         * Configure the work dir to be anything other than the default current working dir (.)
         *
         * @param workDir the new work dir to use
         * @return This builder for further fluent configuration.
         */
        public CommandExecutionBuilder workDir(File workDir) {
            this.workDir = workDir;
            return this;
        }

        /**
         * <p>
         *     Configure the process execution to redirect the error stream to the log.
         * </p>
         *
         * <p>
         *     Default is true when this is not provided.
         * </p>
         *
         * @param redirectError the redirect error value
         * @return This builder for further fluent configuration.
         */
        public CommandExecutionBuilder redirectError(boolean redirectError) {
            this.redirectError = redirectError;
            return this;
        }

        /**
         * <p>
         *     Redirect the stdin of the process execution to the provided {@link InputStream}.
         * </p>
         *
         * <p>
         *     This may be used for instance to provide a password outside of the commandline.
         * </p>
         *
         * @param inputStream the new {@link InputStream}
         * @return This builder for further fluent configuration.
         */
        public CommandExecutionBuilder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        /**
         * Terminated the fluent configuration of the command and compile it to be dispatched to the configued
         * {@link CommandExecutorDelegate}
         *
         * @return A {@link List} of {@link String}s representing the actual output of the command
         * @throws MojoExecutionException In case execution of the command fails
         */
        public List<String> run() throws MojoExecutionException {
            List<String> fullCommand = compileCommandLine(command, subCommands);
            String msg = String.format("Executing command '%s' from basedir %s", StringUtils.join(fullCommand, " "), BASE_DIR.getAbsolutePath());
            log.debug(msg);
            ProcessExecutor processExecutor = new ProcessExecutor()
                    .directory(workDir)
                    .command(fullCommand)
                    .readOutput(true)
                    .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                    .exitValueNormal();

            // Some processes print regular text on stderror, so make redirecting the error stream configurable.
            if (redirectError) {
                processExecutor.redirectError(Slf4jStream.of(getClass().getSimpleName()).asError());
            }

            if (inputStream != null) {
                processExecutor.redirectInput(inputStream);
            }

            return delegate.executeCommand(processExecutor);
        }
    }
}
