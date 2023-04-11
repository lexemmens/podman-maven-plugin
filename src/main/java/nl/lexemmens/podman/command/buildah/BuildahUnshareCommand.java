package nl.lexemmens.podman.command.buildah;

import nl.lexemmens.podman.command.AbstractCommand;
import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the <code>buildah unshare</code> command.
 */
public class BuildahUnshareCommand extends AbstractCommand {

    /**
     * The base command
     */
    private static final String BASE_COMMAND = "buildah";
    private static final String UNSHARE_COMMAND = "unshare";

    private final List<String> command;

    /**
     * Private constructor
     *
     * @param log      The Maven log
     * @param delegate The executor delegate
     */
    private BuildahUnshareCommand(Log log, CommandExecutorDelegate delegate) {
        super(log, delegate);
        this.command = new ArrayList<>();
        this.command.add(BASE_COMMAND);
        this.command.add(UNSHARE_COMMAND);
    }

    @Override
    protected List<String> getCommand() {
        return command;
    }

    @Override
    protected boolean redirectError() {
        return false;
    }

    protected void withOption(String option, String optionValue) {
        final String subCommand;
        if (optionValue == null) {
            subCommand = option;
        } else {
            subCommand = String.format("%s %s", option, optionValue);
        }

        command.add(subCommand);
    }

    /**
     * Builder class to construct an instance of the {@link BuildahUnshareCommand}
     */
    public static class Builder {

        private final BuildahUnshareCommand command;

        /**
         * Constructor. Creates a new instance of this builder
         *
         * @param log      The Maven log
         * @param delegate The executor delegate
         */
        public Builder(Log log, CommandExecutorDelegate delegate) {
            this.command = new BuildahUnshareCommand(log, delegate);
        }

        /**
         * Sets the directory to remove using this command
         *
         * @param directory The directory to remove
         * @return This builder
         */
        public Builder removeDirectory(String directory) {
            this.command.withOption("rm", null);
            this.command.withOption("-rf", null);
            this.command.withOption(directory, null);
            return this;
        }

        /**
         * Returns the constructed instance of the command
         *
         * @return The command
         */
        public Command build() {
            return command;
        }

    }
}
