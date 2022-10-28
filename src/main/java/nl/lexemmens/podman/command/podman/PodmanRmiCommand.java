package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

/**
 * Implementation of the <code>podman rmi</code> command
 */
public class PodmanRmiCommand extends AbstractPodmanCommand {

    private static final String SUBCOMMAND = "rmi";

    private PodmanRmiCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, podmanConfig, delegate, SUBCOMMAND, true);
    }

    /**
     * Builder class for the Podman Rmi command
     */
    public static class Builder {

        private final PodmanRmiCommand command;

        public Builder(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
            this.command = new PodmanRmiCommand(log, podmanConfig, delegate);
        }

        public Builder setFullImageName(String fullImageName) {
            command.withOption(fullImageName, null);
            return this;
        }

        public Command build() {
            return command;
        }

    }
}
