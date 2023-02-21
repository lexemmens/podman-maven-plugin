package nl.lexemmens.podman.command.chcon;

import nl.lexemmens.podman.command.AbstractCommand;
import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Linux <code>chcon</code> command.
 */
public class ChConCommand extends AbstractCommand {

    private static final String BASE_COMMAND = "chcon";

    private final List<String> command;

    /**
     * Private constructor
     *
     * @param log      The Maven log
     * @param delegate The executor delegate
     */
    private ChConCommand(Log log, CommandExecutorDelegate delegate) {
        super(log, delegate);
        this.command = new ArrayList<>();
        this.command.add(BASE_COMMAND);
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
        command.add(option);
        if(optionValue != null) {
            command.add(optionValue);
        }
    }

    /**
     * Builder class to build an instance of this {@link ChConCommand}
     */
    public static class Builder {

        private final ChConCommand command;

        /**
         * Create a new instance of this builder
         *
         * @param log      The Maven Log
         * @param delegate The executor delegate
         */
        public Builder(Log log, CommandExecutorDelegate delegate) {
            this.command = new ChConCommand(log, delegate);
        }

        /**
         * Allows to specify the security context type
         *
         * @param type The security context type
         * @return this builder instance
         */
        public Builder withType(String type) {
            this.command.withOption("-t", type);
            return this;
        }

        /**
         * Specifies the directory for which the security context type should be changed
         *
         * @param directory The directory for which the security context should be changed
         * @return This builder instance
         */
        public Builder withDirectory(String directory) {
            this.command.withOption(directory, null);
            return this;
        }

        /**
         * Returns the constructed command
         *
         * @return The constructed command
         */
        public Command build() {
            return command;
        }

    }
}
