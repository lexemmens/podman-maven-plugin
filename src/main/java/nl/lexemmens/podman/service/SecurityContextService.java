package nl.lexemmens.podman.service;


import nl.lexemmens.podman.command.Command;
import nl.lexemmens.podman.command.chcon.ChConCommand;
import nl.lexemmens.podman.command.podman.PodmanSystemCommand;
import nl.lexemmens.podman.command.sestatus.SeStatusCommand;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.executor.CommandExecutorDelegate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SecurityContextService {

    private static final String TARGET_SECURITY_CONTEXT_TYPE = "data_home_t";

    private static final Pattern SELINUX_STATUS_REGEX = Pattern.compile("(SELinux status:\\s*)(enabled|disabled)");

    private static final Pattern GRAPH_ROOT_PATTERN = Pattern.compile("(graphRoot: )([a-zA-Z\\/\\.]+)");
    private static final String UNKNOWN = "unknown";

    private final Log log;
    private final PodmanConfiguration podmanCfg;
    private final CommandExecutorDelegate delegate;

    public SecurityContextService(Log log, PodmanConfiguration podmanConfiguration, CommandExecutorDelegate delegate) {
        this.podmanCfg = podmanConfiguration;
        this.log = log;
        this.delegate = delegate;
    }

    public void setSecurityContext() throws MojoExecutionException {
        log.debug("Checking SELinux status...");
        boolean seLinuxEnabled = isSELinuxEnabled();

        if (seLinuxEnabled) {
            log.debug("SELinux is enabled");
            doSetSecurityContext();
        } else {
            log.debug("Not setting security context because SELinux is disabled.");
        }
    }

    private boolean isSELinuxEnabled() throws MojoExecutionException {
        Command seStatusCommand = new SeStatusCommand.Builder(log, delegate).build();

        /*
         * Output should be similar to
         * $ sestatus
         * SELinux status:                 enabled
         * SELinuxfs mount:                /sys/fs/selinux
         * SELinux root directory:         /etc/selinux
         * Loaded policy name:             targeted
         * Current mode:                   enforcing
         * Mode from config file:          enforcing
         * Policy MLS status:              enabled
         * Policy deny_unknown status:     allowed
         * Memory protection checking:     actual (secure)
         * Max kernel policy version:      33
         */
        Optional<String> seLinuxStatus = seStatusCommand.execute()
                .stream()
                .filter(line -> line.contains("SELinux status"))
                .map(this::extractSeLinuxStatus)
                .findFirst();

        return seLinuxStatus.map(seLinuxStatusString -> seLinuxStatusString.equals("enabled")).orElse(false);
    }

    private String extractSeLinuxStatus(String line) {
        Matcher seLinuxStatusMatcher = SELINUX_STATUS_REGEX.matcher(line);
        if (seLinuxStatusMatcher.matches()) {
            return seLinuxStatusMatcher.group(2);
        } else {
            log.warn("Unable to determine if SELinux is enabled! Continuing without setting proper security context.");
            return UNKNOWN;
        }
    }

    private void doSetSecurityContext() throws MojoExecutionException {
        if (podmanCfg.getRoot() == null) {
            log.debug("Using Podman default storage location. Assuming security context is set correctly " +
                    "for this location. Refer to the documentation of this plugin if you run into any issues.");
        } else {
            log.debug("Using custom root with SELinux enabled. Setting security context to " + TARGET_SECURITY_CONTEXT_TYPE + " for " + podmanCfg.getRoot());
            // In order to set the context, we need to ensure that the destination folder exists.
            try {
                Path path = podmanCfg.getRoot().toPath().normalize();
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to set security context on Podman's (custom) root location: " + podmanCfg.getRoot().getAbsolutePath(),
                        e
                );
            }

            List<String> graphRootCmdOutput = new PodmanSystemCommand.Builder(log, podmanCfg, delegate)
                    .info()
                    .build()
                    .execute()
                    .stream()
                    .filter(output -> output.matches("(graphRoot: )([a-zA-Z\\/\\.]+)"))
                    .collect(Collectors.toList());

            if(graphRootCmdOutput.size() == 1) {
                String graphRoot = graphRootCmdOutput.get(0);
                Matcher graphRootMatcher = GRAPH_ROOT_PATTERN.matcher(graphRoot);
                if(graphRootMatcher.matches()) {
                    String graphRootLocation = graphRootMatcher.group(2);
                    log.info("Determined graphRoot location to be: " + graphRootLocation + ". Executing chcon using this directory as reference...");
                    executeChConCommand(graphRootLocation);
                } else {
                    log.warn("Failed to determine Podman's storage location from output " + graphRoot);
                }
            } else {
                log.warn("Failed to determine Podman's storage location. If SELinux is enabled, this may cause unexpected behaviour.");
            }
        }
    }

    private void executeChConCommand(String referenceStorageLocation) throws MojoExecutionException {
        // If the directory is created, set the security context
        Command chconCommand = new ChConCommand.Builder(log, delegate)
                .withRecursiveOption()
                .withReferenceDirectory(referenceStorageLocation, podmanCfg.getRoot().getAbsolutePath())
                .build();

        chconCommand.execute();
    }


}
