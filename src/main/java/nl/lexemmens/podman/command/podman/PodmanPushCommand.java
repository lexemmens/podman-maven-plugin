package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

/**
 * Implementation of the <code>podman push</code> command
 */
public class PodmanPushCommand extends AbstractPodmanCommand {

    private static final String SUBCOMMAND = "push";

    private PodmanPushCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, podmanConfig, delegate, SUBCOMMAND, false);
    }

    /**
     * Builder class for the Podman Push command
     */
    public static class Builder {

        private final PodmanPushCommand command;

        public Builder(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
            this.command = new PodmanPushCommand(log, podmanConfig, delegate);
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
