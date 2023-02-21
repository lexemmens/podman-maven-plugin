package nl.lexemmens.podman.command.sestatus;

import nl.lexemmens.podman.command.AbstractCommand;
import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of the <code>sestatus</code> command
 */
public class SeStatusCommand extends AbstractCommand {

    private static final String SESTATUS_COMMAND = "sestatus";

    private SeStatusCommand(Log log, CommandExecutorDelegate delegate) {
        super(log, delegate);
    }

    @Override
    protected List<String> getCommand() {
        return Collections.singletonList(SESTATUS_COMMAND);
    }

    @Override
    protected boolean redirectError() {
        return false;
    }


    /**
     * Builder class for the SeStatus command
     */
    public static class Builder {

        private final SeStatusCommand command;

        public Builder(Log log, CommandExecutorDelegate delegate) {
            this.command = new SeStatusCommand(log, delegate);
        }

        public Command build() {
            return command;
        }

    }


}
