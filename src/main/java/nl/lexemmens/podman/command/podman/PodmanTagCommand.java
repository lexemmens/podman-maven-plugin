package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

/**
 * Implementation of the <code>podman tag</code> command
 */
public class PodmanTagCommand extends AbstractPodmanCommand {

    private static final String SUBCOMMAND = "tag";

    private PodmanTagCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, podmanConfig, delegate, SUBCOMMAND, true);
    }

    /**
     * Builder class for the Podman Tag command
     */
    public static class Builder {

        private final PodmanTagCommand command;

        public Builder(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
            this.command = new PodmanTagCommand(log, podmanConfig, delegate);
        }

        public Builder setImageHash(String imageHash) {
            command.withOption(imageHash, null);
            return this;
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
