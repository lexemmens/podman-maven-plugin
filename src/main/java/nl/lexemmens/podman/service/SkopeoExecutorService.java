package nl.lexemmens.podman.service;

import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.enumeration.SkopeoCommand;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Enables executing the skopeo binary with specific arguments.
 */
public class SkopeoExecutorService {
    private static final File BASE_DIR = new File(".");
    private static final String SRC_TLS_VERIFY_CMD = "--src-tls-verify=";
    private static final String DEST_TLS_VERIFY_CMD = "--dest-tls-verify=";
    private static final String IMAGE_PREFIX = "docker://";

    private final Log log;
    private final CommandExecutorDelegate delegate;
    private final SkopeoConfiguration skopeoConfiguration;

    /**
     * Constructs a new instance of this class.
     *
     * @param log          Used to access Maven's log system
     * @param skopeoConfiguration Contains skopeo specific configuration, such as getSrcTlsVerify
     * @param delegate     A delegate executor that executed the actual command
     */
    public SkopeoExecutorService(Log log, SkopeoConfiguration skopeoConfiguration, CommandExecutorDelegate delegate) {
        this.log = log;
        this.skopeoConfiguration = skopeoConfiguration;
        this.delegate = delegate;
    }

    /**
     * Implementation of the skopeo copy command.
     *
     * @param sourceImage source image to copy
     * @param destinationImage target for the image
     * @throws MojoExecutionException In case the skopeo copy command exits unsuccessfully.
     */
    public void copy(String sourceImage, String destinationImage) throws MojoExecutionException {
        List<String> subCommands = new ArrayList<>();

        subCommands.add(SRC_TLS_VERIFY_CMD + skopeoConfiguration.getCopy().getSrcTlsVerify());
        subCommands.add(DEST_TLS_VERIFY_CMD + skopeoConfiguration.getCopy().getDestTlsVerify());

        subCommands.add(IMAGE_PREFIX + sourceImage);
        subCommands.add(IMAGE_PREFIX + destinationImage);

        runCommand(SkopeoCommand.COPY, subCommands);
    }

    private List<String> decorateCommands(SkopeoCommand skopeoCommand, List<String> subCommands) {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(SkopeoCommand.SKOPEO.getCommand());
        fullCommand.add(skopeoCommand.getCommand());
        fullCommand.addAll(subCommands);

        return fullCommand;
    }

    private List<String> runCommand(SkopeoCommand command, List<String> subCommands) throws MojoExecutionException {
        List<String> fullCommand = decorateCommands(command, subCommands);

        String msg = String.format("Executing command '%s' from basedir %s", StringUtils.join(fullCommand, " "), BASE_DIR.getAbsolutePath());
        log.debug(msg);
        ProcessExecutor processExecutor = new ProcessExecutor()
                .directory(BASE_DIR)
                .command(fullCommand)
                .readOutput(true)
                .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                .redirectError(Slf4jStream.of(getClass().getSimpleName()).asError())
                .exitValueNormal();

        return delegate.executeCommand(processExecutor);
    }
}
