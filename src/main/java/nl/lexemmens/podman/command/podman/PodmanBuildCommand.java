package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

import java.nio.file.Path;

/**
 * Implementation of the <code>podman build</code> command
 */
public class PodmanBuildCommand extends AbstractPodmanCommand {

    private static final String SQUASH_CMD = "--squash";
    private static final String SQUASH_ALL_CMD = "--squash-all";
    private static final String LAYERS_CMD = "--layers";
    private static final String BUILD_FORMAT_CMD = "--format";
    private static final String CONTAINERFILE_CMD = "--file";
    private static final String PULL_CMD = "--pull";
    private static final String PULL_ALWAYS_CMD = "--pull-always";
    private static final String NO_CACHE_CMD = "--no-cache";
    private static final String SUBCOMMAND = "build";

    private PodmanBuildCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, podmanConfig, delegate, SUBCOMMAND, false);
    }

    /**
     * Builder class used to create an instance of the {@link PodmanBuildCommand}
     */
    public static class Builder {

        private final PodmanBuildCommand command;

        /**
         * Construct a new instance of this builder
         *
         * @param log          The Maven log
         * @param podmanConfig The Podman configuration
         * @param delegate     The executor delegate
         */
        public Builder(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
            this.command = new PodmanBuildCommand(log, podmanConfig, delegate);
        }

        /**
         * Sets whether all layers should be squashed
         *
         * @return This builder instance
         */
        public Builder setSquash() {
            command.withOption(SQUASH_CMD, null);
            return this;
        }

        /**
         * Sets whether all layers should be squashed
         *
         * @return This builder instance
         */
        public Builder setSquashAll() {
            command.withOption(SQUASH_ALL_CMD, null);
            return this;
        }

        /**
         * Sets the layers option
         *
         * @param layers Sets the value of the layers option
         * @return This builder instance
         */
        public Builder setLayers(Boolean layers) {
            command.withOption(LAYERS_CMD, layers.toString());
            return this;
        }

        /**
         * Sets the format of the containers metadata
         *
         * @param format The value of the format option to set
         * @return This builder instance
         */
        public Builder setFormat(String format) {
            command.withOption(BUILD_FORMAT_CMD, format);
            return this;
        }

        /**
         * Sets the Containerfile that should be build
         *
         * @param containerFile The pointer towards the containerfile to use
         * @return This builder instance
         */
        public Builder setContainerFile(Path containerFile) {
            command.withOption(CONTAINERFILE_CMD, containerFile.toString());
            return this;
        }

        /**
         * Sets whether the base image should be pulled
         *
         * @param pull Sets whether to pull the image
         * @return This builder instance
         */
        public Builder setPull(Boolean pull) {
            command.withOption(PULL_CMD, pull.toString());
            return this;
        }

        /**
         * Sets whether or not to cache intermediate layers
         *
         * @param noCache Sets whether the noCache option should be used
         * @return This builder instance
         */
        public Builder setNoCache(boolean noCache) {
            command.withOption(NO_CACHE_CMD, "" + noCache);
            return this;
        }

        /**
         * Sets whether base images should always be pushed
         *
         * @param pullAlways Sets the value of the pullAlways property
         * @return This builder instance
         */
        public Builder setPullAllways(Boolean pullAlways) {
            command.withOption(PULL_ALWAYS_CMD, pullAlways.toString());
            return this;
        }

        /**
         * Returns the constructed command
         *
         * @return The constructed command
         */
        public Command build() {
            // Add current directory
            command.withOption(".", null);
            return command;
        }

    }


}
