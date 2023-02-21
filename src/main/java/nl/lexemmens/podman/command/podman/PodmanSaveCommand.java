package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

/**
 * Implementation of the <code>podman save</code> command
 */
public class PodmanSaveCommand extends AbstractPodmanCommand {

    private static final String SAVE_FORMAT_CMD = "--format";
    private static final String OCI_ARCHIVE = "oci-archive";
    private static final String OUTPUT_CMD = "--output";

    private static final String SUBCOMMAND = "save";

    private PodmanSaveCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, podmanConfig, delegate, SUBCOMMAND, true);
    }

    /**
     * Builder class for the Podman Save command
     */
    public static class Builder {

        private final PodmanSaveCommand command;

        public Builder(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
            this.command = new PodmanSaveCommand(log, podmanConfig, delegate);
            this.command.withOption(SAVE_FORMAT_CMD, OCI_ARCHIVE);
        }

        public Builder setArchiveName(String archiveName) {
            command.withOption(OUTPUT_CMD, archiveName);
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
