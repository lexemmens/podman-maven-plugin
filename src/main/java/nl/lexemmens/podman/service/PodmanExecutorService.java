package nl.lexemmens.podman.service;

import nl.lexemmens.podman.enumeration.PodmanCommand;
import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import nl.lexemmens.podman.image.ImageConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PodmanExecutorService {

    private static final String SAVE_FORMAT_CMD = "--format=oci-archive";
    private static final String OUTPUT_CMD = "--output";
    private static final String DOCKERFILE_CMD = "--file=";
    private static final String NO_CACHE_CMD = "--no-cache=";

    private static final File BASE_DIR = new File(".");

    private final Log log;
    private final TlsVerify tlsVerify;
    private final CommandExecutorDelegate delegate;

    public PodmanExecutorService(Log log, TlsVerify tlsVerify, CommandExecutorDelegate delegate) {
        this.log = log;
        this.tlsVerify = tlsVerify;
        this.delegate = delegate;
    }

    public String build(ImageConfiguration image) throws MojoExecutionException {
        List<String> subCommand = new ArrayList<>();
        subCommand.add(DOCKERFILE_CMD + image.getBuild().getTargetDockerfile());
        subCommand.add(NO_CACHE_CMD + image.getBuild().isNoCache());
        subCommand.add(".");

        List<String> processOutput = runCommand(image.getBuild().getOutputDirectory(), false, PodmanCommand.BUILD, subCommand);
        return processOutput.get(processOutput.size() - 1);
    }

    public void tag(String imageHash, String fullImageName) throws MojoExecutionException {
        // Ignore output
        runCommand(PodmanCommand.TAG, List.of(imageHash, fullImageName));
    }

    public void save(String archiveName, String fullImageName) throws MojoExecutionException {
        List<String> subCommand = new ArrayList<>();
        subCommand.add(SAVE_FORMAT_CMD);
        subCommand.add(OUTPUT_CMD);
        subCommand.add(archiveName);
        subCommand.add(fullImageName);

        runCommand(PodmanCommand.SAVE, subCommand);
    }

    public void push(String fullImageName) throws MojoExecutionException {
        // Apparently, actually pushing the blobs to a registry causes some output on stderr.
        // Ignore output
        runCommand(BASE_DIR, false, PodmanCommand.PUSH, List.of(fullImageName));
    }

    public void login(String registry, String username, String password) throws MojoExecutionException {
        List<String> subCommand = new ArrayList<>();
        subCommand.add(registry);
        subCommand.add("-u");
        subCommand.add(username);
        subCommand.add("-p");
        subCommand.add(password);

        try {
            runCommand(PodmanCommand.LOGIN, subCommand);
        } catch (MojoExecutionException e) {
            // When the command fails, the whole command is put in the error message, possibly exposing passwords.
            // Therefore we catch the exception, remove the password and throw a new exception with an updated message.
            String message = e.getMessage().replaceAll(String.format("-p %s", password), "-p *****");
            log.error(message);
            throw new MojoExecutionException(message);
        }
    }

    public void removeLocalImage(String fullImageName) throws MojoExecutionException {
        runCommand(PodmanCommand.RMI, List.of(fullImageName));
    }

    private List<String> decorateCommands(PodmanCommand podmanCommand, List<String> subCommands) {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(PodmanCommand.PODMAN.getCommand());
        fullCommand.add(podmanCommand.getCommand());

        if (!PodmanCommand.TAG.equals(podmanCommand)
                && !PodmanCommand.SAVE.equals(podmanCommand)
                && !PodmanCommand.RMI.equals(podmanCommand)
                && tlsVerify != null) {
            fullCommand.add(tlsVerify.getCommand());
        }

        fullCommand.addAll(subCommands);

        return fullCommand;
    }

    private List<String> runCommand(File baseDir, boolean redirectError, PodmanCommand command, List<String> subCommands) throws MojoExecutionException {
        String msg = String.format("Executing command %s from basedir %s", StringUtils.join(command, " "), baseDir);
        log.debug(msg);
        ProcessExecutor processExecutor = new ProcessExecutor()
                .directory(baseDir)
                .command(decorateCommands(command, subCommands))
                .readOutput(true)
                .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                .exitValueNormal();

        // Some processes print regular text on stderror, so make redirecting the error stream configurable.
        if (redirectError) {
            processExecutor.redirectError(Slf4jStream.of(getClass().getSimpleName()).asError());
        }

        return delegate.executeCommand(processExecutor);

    }

    private void runCommand(PodmanCommand command, List<String> subCommands) throws MojoExecutionException {
        // Ignore output
        runCommand(BASE_DIR, true, command, subCommands);
    }
}
