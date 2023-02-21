package nl.lexemmens.podman.command.podman;

import nl.lexemmens.podman.command.AbstractCommand;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static nl.lexemmens.podman.enumeration.TlsVerify.NOT_SPECIFIED;

/**
 * Base class for all Podman commands
 */
public abstract class AbstractPodmanCommand extends AbstractCommand {

    private static final String BASE_COMMAND = "podman";
    private static final String ROOT_CMD = "--root=";
    private static final String RUNROOT_CMD = "--runroot=";

    private final boolean redirectError;
    private final List<String> command;

    private final TlsVerify tlsVerify;
    private final File podmanRoot;
    private final File podmanRunRoot;

    /**
     * Constructs a new instance of this {@link AbstractPodmanCommand}
     *
     * @param log           The Maven log
     * @param podmanConfig  The Podman configuration in use
     * @param delegate      The executor delegate
     * @param subCommand    The subcmmand to execute, i.e. build or push
     * @param redirectError Whether or not to redirect errors
     */
    protected AbstractPodmanCommand(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate, String subCommand, boolean redirectError) {
        super(log, delegate, podmanConfig.getRunDirectory());

        this.tlsVerify = podmanConfig.getTlsVerify();
        this.podmanRoot = podmanConfig.getRoot();
        this.podmanRunRoot = podmanConfig.getRunRoot();

        this.redirectError = redirectError;
        this.command = createBaseCommand(subCommand);
    }

    @Override
    protected List<String> getCommand() {
        return command;
    }

    @Override
    protected boolean redirectError() {
        return redirectError;
    }

    /**
     * Appends an option to this command
     *
     * @param option      The option
     * @param optionValue The option value
     */
    protected void withOption(String option, String optionValue) {
        final String subCommand;
        if (optionValue == null) {
            subCommand = option;
        } else {
            subCommand = String.format("%s=%s", option, optionValue);
        }

        command.add(subCommand);
    }

    private List<String> createBaseCommand(String subCommand) {
        List<String> baseCommand = new ArrayList<>();
        baseCommand.add(BASE_COMMAND);

        // Path to the root directory in which data, including images, is stored. Must be *before* build, push or any other operation
        if (podmanRoot != null) {
            baseCommand.add(ROOT_CMD + podmanRoot.getAbsolutePath());
        }

        if (podmanRunRoot != null) {
            baseCommand.add(RUNROOT_CMD + podmanRunRoot.getAbsolutePath());
        }

        baseCommand.add(subCommand);

        if (isTlsSupported(subCommand) && tlsVerify != null && !NOT_SPECIFIED.equals(tlsVerify)) {
            baseCommand.add(tlsVerify.getCommand());
        }

        return baseCommand;
    }

    private boolean isTlsSupported(String subCommand) {
        return !"version".equals(subCommand)
                && !"tag".equals(subCommand)
                && !"save".equals(subCommand)
                && !"rmi".equals(subCommand);
    }

}
