package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHub;
import nl.lexemmens.podman.service.CommandExecutorService;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "push", defaultPhase = LifecyclePhase.INSTALL)
public class PushMojo extends AbstractPodmanMojo {

    private static final String PODMAN = "podman";
    private static final String PUSH = "push";

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.skip.push", required = true)
    private boolean skipPush;

    @Override
    public void executeInternal(ServiceHub ctx) throws MojoExecutionException {
        if (skipPush) {
            getLog().info("Pushing container images is skipped.");
        } else if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Will not push container images.");
        } else {
            getLog().info("Pushing container images to registry ...");

            for (String tag : tags) {
                getLog().info("Pushing image: " + tag);
                ctx.getCommandExecutorService().runCommand(outputDirectory, PODMAN, PUSH, tag);
            }
        }
    }
}
