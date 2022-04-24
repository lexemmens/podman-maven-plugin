package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.enumeration.PodmanCommand;
import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static nl.lexemmens.podman.enumeration.TlsVerify.NOT_SPECIFIED;

/**
 * Class that allows very specific execution of Podman related commands.
 */
public class PodmanExecutorService extends AbstractExecutorService<PodmanCommand> {

    private static final String BUILD_FORMAT_CMD = "--format=";
    private static final String LAYERS_CMD = "--layers=";
    private static final String SAVE_FORMAT_CMD = "--format=oci-archive";
    private static final String OUTPUT_CMD = "--output";
    private static final String CONTAINERFILE_CMD = "--file=";
    private static final String NO_CACHE_CMD = "--no-cache=";
    private static final String PASSWORD_STDIN_CMD = "--password-stdin";
    private static final String PULL_CMD = "--pull=";
    private static final String PULL_ALWAYS_CMD = "--pull-always=";
    private static final String ROOT_CMD = "--root=";
    private static final String RUNROOT_CMD = "--runroot=";
    private static final String SQUASH_CMD = "--squash";
    private static final String SQUASH_ALL_CMD = "--squash-all";
    private static final String USERNAME_CMD = "--username=";


    private final TlsVerify tlsVerify;
    private final File podmanRoot;
    private final File podmanRunRoot;
    private final File podmanRunDirectory;

    /**
     * Constructs a new instance of this class.
     *
     * @param log          Used to access Maven's log system
     * @param podmanConfig Contains Podman specific configuration, such as tlsVerify and podman's root directory
     * @param delegate     A delegate executor that executed the actual command
     */
    public PodmanExecutorService(Log log, PodmanConfiguration podmanConfig, CommandExecutorDelegate delegate) {
        super(log, delegate);
        this.tlsVerify = podmanConfig.getTlsVerify();
        this.podmanRoot = podmanConfig.getRoot();
        this.podmanRunRoot = podmanConfig.getRunRoot();
        this.podmanRunDirectory = podmanConfig.getRunDirectory();
    }

    /**
     * <p>
     * Implementation of the 'podman build' command.
     * </p>
     * <p>
     * Takes an {@link SingleImageConfiguration} class as input and uses it to retrieve
     * the Containerfile to build, whether caching should be used and the build's output directory
     * </p>
     *
     * @param image The {@link SingleImageConfiguration} containing the configuration of the image to build
     * @return The last line of the build process, usually containing the image hash
     * @throws MojoExecutionException In case the container image could not be built.
     */
    public List<String> build(SingleImageConfiguration image) throws MojoExecutionException {
        AbstractExecutorService<PodmanCommand>.CommandExecutionBuilder command = command(PodmanCommand.BUILD);

        if(Boolean.TRUE == image.getBuild().getSquash()) {
            command.subCommand(SQUASH_CMD);
        }

        if(Boolean.TRUE == image.getBuild().getSquashAll()) {
            command.subCommand(SQUASH_ALL_CMD);
        }

        if(image.getBuild().getLayers() != null) {
            command.subCommand(LAYERS_CMD + image.getBuild().getLayers());
        }

        command.subCommand(BUILD_FORMAT_CMD + image.getBuild().getFormat().getValue());
        command.subCommand(CONTAINERFILE_CMD + image.getBuild().getTargetContainerFile());
        command.subCommand(NO_CACHE_CMD + image.getBuild().isNoCache());

        if(image.getBuild().getPull().isPresent()) {
            command.subCommand(PULL_CMD + image.getBuild().getPull().get());
        }

        if(image.getBuild().getPullAlways().isPresent()) {
            command.subCommand(PULL_ALWAYS_CMD + image.getBuild().getPullAlways().get());
        }

        return command.subCommand(".")
                .workDir(podmanRunDirectory)
                .redirectError(false)
                .run();
    }

    /**
     * <p>
     * Implementation of the 'podman tag' command.
     * </p>
     *
     * @param imageHash     The image hash as generated by the {@link #build(SingleImageConfiguration)} method
     * @param fullImageName The full name of the image. This will be the target name
     * @throws MojoExecutionException In case the container image could not be tagged.
     */
    public void tag(String imageHash, String fullImageName) throws MojoExecutionException {
        // Ignore output
        command(PodmanCommand.TAG)
                .subCommand(imageHash)
                .subCommand(fullImageName)
                .run();
    }

    /**
     * <p>
     * Implementation of the 'podman save' command.
     * </p>
     * <p>
     * Note: This is not an export. The result of the save command is a tar ball containing all layers
     * as separate folders
     * </p>
     *
     * @param archiveName   The target name of the archive, where the image will be saved into.
     * @param fullImageName The image to save
     * @throws MojoExecutionException In case the container image could not be saved.
     */
    public void save(String archiveName, String fullImageName) throws MojoExecutionException {
        // Ignore output
        command(PodmanCommand.SAVE)
                .subCommand(SAVE_FORMAT_CMD)
                .subCommand(OUTPUT_CMD)
                .subCommand(archiveName)
                .subCommand(fullImageName)
                .run();
    }

    /**
     * <p>
     * Implementation of the 'podman push' command.
     * </p>
     *
     * @param fullImageName The full name of the image including the registry
     * @throws MojoExecutionException In case the container image could not be pushed.
     */
    public void push(String fullImageName) throws MojoExecutionException {
        // Apparently, actually pushing the blobs to a registry causes some output on stderr.
        // Ignore output
        command(PodmanCommand.PUSH)
                .subCommand(fullImageName)
                .redirectError(false)
                .run();
    }

    /**
     * <p>
     * Implementation of the 'podman login' command.
     * </p>
     * <p>
     * This command is used to login to a specific registry with a specific username and password
     * </p>
     *
     * @param registry The registry to logon to
     * @param username The username to use
     * @param password The password to use
     * @throws MojoExecutionException In case the login fails. The Exception does not contain a recognisable password.
     */
    public void login(String registry, String username, String password) throws MojoExecutionException {
        try (InputStream inputStream = new ByteArrayInputStream(password.getBytes(StandardCharsets.UTF_8))) {
            command(PodmanCommand.LOGIN)
                    .subCommand(registry)
                    .subCommand(USERNAME_CMD + username)
                    .subCommand(PASSWORD_STDIN_CMD)
                    .inputStream(inputStream)
                    .redirectError(true)
                    .run();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to provide password over stdin", e);
        }
    }

    /**
     * <p>
     * Implementation of the 'podman version' command
     * </p>
     *
     * @throws MojoExecutionException In case printing the information fails
     */
    public void version() throws MojoExecutionException {
        command(PodmanCommand.VERSION)
                .run();
    }

    /**
     * <p>
     * Implementation of the 'podman rmi' command.
     * </p>
     *
     * <p>
     * Removes an image from the local registry
     * </p>
     *
     * @param fullImageName The full name of the image to remove from the local registry
     * @throws MojoExecutionException In case the container image could not be removed.
     */
    public void removeLocalImage(String fullImageName) throws MojoExecutionException {
        //Ignore output
        command(PodmanCommand.RMI)
                .subCommand(fullImageName)
                .run();
    }

    @Override
    protected List<String> compileCommandLine(PodmanCommand podmanCommand, List<String> subCommands) {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(PodmanCommand.PODMAN.getCommand());

        // Path to the root directory in which data, including images, is stored. Must be *before* build, push or any other operation
        if (podmanCommand.isRunRootSupported() && podmanRoot != null) {
            fullCommand.add(ROOT_CMD + podmanRoot.getAbsolutePath());
        }

        if (podmanCommand.isRunRootSupported() && podmanRunRoot != null) {
            fullCommand.add(RUNROOT_CMD + podmanRunRoot.getAbsolutePath());
        }

        fullCommand.add(podmanCommand.getCommand());

        if (podmanCommand.isTlsSupported() && tlsVerify != null && !NOT_SPECIFIED.equals(tlsVerify)) {
            fullCommand.add(tlsVerify.getCommand());
        }

        fullCommand.addAll(subCommands);
        return fullCommand;
    }
}
