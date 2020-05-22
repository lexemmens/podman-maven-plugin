package nl.lexemmens.podman;

import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * PushMojo for pushing container images to a registry/repository
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class PushMojo extends AbstractPodmanMojo {

    private static final String PUSH = "push";
    private static final String REMOVE_LOCAL = "rmi";

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.skip.push", required = true)
    private boolean skipPush;

    /**
     * Decides whether the local image should be deleted after pushing to the registry. Defaults to false.
     * Note: When set to true, only the created image is deleted. Cached base images may continue to exist on the operating system
     */
    @Parameter(property = "podman.image.delete.after.push", defaultValue = "false", required = true)
    protected boolean deleteLocalImageAfterPush;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipPush) {
            getLog().info("Pushing container images is skipped.");
        } else if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Will not push container images.");
        } else {
            exportContainerImages(hub);
        }
    }

    private void exportContainerImages(ServiceHub hub) throws MojoExecutionException {
        getLog().info("Pushing container images to registry ...");

        ImageConfiguration imageConfiguration = getImageConfiguration();
        if(imageConfiguration.getFullImageNames().isEmpty()) {
            getLog().info("There are no container images to be pushed. Consider running 'mvn install' first.");
        } else {
            for (String tag : imageConfiguration.getFullImageNames()) {
                getLog().info("Pushing image: " + tag + " to " + imageConfiguration.getRegistry());

                hub.getCommandExecutorService().runCommand(outputDirectory, true, false, PODMAN, PUSH, tlsVerify.getCommand(), tag);
                // Apparently, actually pushing the blobs to a registry causes some output on stderr.

                if (deleteLocalImageAfterPush) {
                    getLog().info("Removing image " + tag + " from the local repository");
                    hub.getCommandExecutorService().runCommand(outputDirectory, PODMAN, REMOVE_LOCAL, tag);
                }
            }
        }

        getLog().info("All images have been successfully pushed to the registry");
    }
}
