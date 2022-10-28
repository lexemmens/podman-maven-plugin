package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

/**
 * Implementation of the <code>podman login</code> command
 */
public class PodmanLoginCommand extends AbstractPodmanCommand {

    private static final String USERNAME_OPTION = "-u";
    private static final String PASSWORD_OPTION = "-p";

    private static final String SUBCOMMAND = "login";

    private PodmanLoginCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, podmanConfig, delegate, SUBCOMMAND, true);
    }

    /**
     * Builder class for the Podman Login command
     */
    public static class Builder {

        private final PodmanLoginCommand command;

        public Builder(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
            this.command = new PodmanLoginCommand(log, podmanConfig, delegate);
        }

        public Builder setRegistry(String registry) {
            command.withOption(registry, null);
            return this;
        }

        public Builder setUsername(String username) {
            command.withOption(USERNAME_OPTION, username);
            return this;
        }

        public Builder setPassword(String password) {
            command.withOption(PASSWORD_OPTION, password);
            return this;
        }

        public Command build() {
            return command;
        }

    }
}
