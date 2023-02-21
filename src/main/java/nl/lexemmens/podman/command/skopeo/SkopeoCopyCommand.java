package nl.lexemmens.podman.command.skopeo;

import nl.lexemmens.podman.command.AbstractCommand;
import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the <code>skopeo copy</code> command
 */
public class SkopeoCopyCommand extends AbstractCommand {

    private static final String SRC_TLS_VERIFY_CMD = "--src-tls-verify";
    private static final String DEST_TLS_VERIFY_CMD = "--dest-tls-verify";
    private static final String IMAGE_PREFIX = "docker://";

    private static final String BASE_COMMAND = "skopeo";

    private final List<String> command;

    private SkopeoCopyCommand(Log log, SkopeoConfiguration skopeoConfig, CommandExecutorDelegate delegate) {
        super(log, delegate);
        this.command = new ArrayList<>();
        this.command.add(BASE_COMMAND);

        withOption("copy", null);
        withOption(SRC_TLS_VERIFY_CMD, "" + skopeoConfig.getCopy().getSrcTlsVerify());
        withOption(DEST_TLS_VERIFY_CMD, "" + skopeoConfig.getCopy().getDestTlsVerify());
    }

    @Override
    protected List<String> getCommand() {
        return command;
    }

    @Override
    protected boolean redirectError() {
        return false;
    }

    /**
     * Specifies a specific option to pass to the skopeo copy command.
     */
    private void withOption(String option, String optionValue) {
        final String subCommand;
        if (optionValue == null) {
            subCommand = option;
        } else {
            subCommand = String.format("%s=%s", option, optionValue);
        }

        command.add(subCommand);
    }

    /**
     * Builder class for the Skopeo Copy command
     */
    public static class Builder {

        private final SkopeoCopyCommand command;

        /**
         * Constructor. Creates a new instance of the builder for the skopeop copy command
         */
        public Builder(Log log, SkopeoConfiguration skopeoConfig, CommandExecutorDelegate delegate) {
            this.command = new SkopeoCopyCommand(log, skopeoConfig, delegate);
        }

        /**
         * Sets the source image to copy
         */
        public Builder setSourceImage(String fullImageName) {
            setImage(fullImageName);
            return this;
        }

        /**
         * Sets the destination location of the image
         */
        public Builder setDestinationImage(String fullImageName) {
            setImage(fullImageName);
            return this;
        }

        private void setImage(String fullImageName) {
            this.command.withOption(IMAGE_PREFIX + fullImageName, null);
        }

        /**
         * Builds the skopeo copy command
         */
        public Command build() {
            return command;
        }

    }
}
