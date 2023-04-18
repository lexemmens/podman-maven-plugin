package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

/**
 * Implementation of the <code>podman version</code> command
 */
public class PodmanSystemCommand extends AbstractPodmanCommand {

    private static final String SUBCOMMAND = "system";

    private PodmanSystemCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, podmanConfig, delegate, SUBCOMMAND, true);
    }

    /**
     * Builder class for the Podman Version command
     */
    public static class Builder {

        private final PodmanSystemCommand command;

        public Builder(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
            this.command = new PodmanSystemCommand(log, podmanConfig, delegate);
        }

        public Builder info() {
            command.withOption("info", null);
            return this;
        }

        public Command build() {
            return command;
        }

    }
}
