package nl.lexemmens.podman;

import nl.lexemmens.podman.config.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "push", defaultPhase = LifecyclePhase.INSTALL)
public class PushMojo extends AbstractPodmanMojo {

    private static final String PODMAN = "podman";
    private static final String PUSH = "push";
    private static final String TLS_VERIFY_CMD = "--tls-verify=";

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.skip.push", required = true)
    private boolean skipPush;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipPush) {
            getLog().info("Pushing container images is skipped.");
        } else if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Will not push container images.");
        } else {
            getLog().info("Pushing container images to registry ...");

            ImageConfiguration imageConfiguration = getImageConfiguration();
            for (String tag : imageConfiguration.getFullImageNames()) {
                getLog().info("Pushing image: " + tag + " to " + imageConfiguration.getRegistry());

                String tlsVerifyOption = TLS_VERIFY_CMD + tlsVerify;
                hub.getCommandExecutorService().runCommand(outputDirectory, false, PODMAN, PUSH, tlsVerifyOption, tag);
                // Apparently, actually pushing the blobs to a registry causes some output on stderr.
            }

            // TODO Images are still stored locally, so clean up?
            getLog().info("Images successfully pushed to the registry");
        }
    }
}
