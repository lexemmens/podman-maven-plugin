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
    boolean skipPush;

    /**
     * Decides whether the local image should be deleted after pushing to the registry. Defaults to false.
     * Note: When set to true, only the created image is deleted. Cached base images may continue to exist on the operating system
     */
    @Parameter(property = "podman.image.delete.after.push", defaultValue = "false", required = true)
    boolean deleteLocalImageAfterPush;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipPush) {
            getLog().info("Pushing container images is skipped.");
        }

        for(ImageConfiguration image : images) {
            if (image.getBuild().getAllTags().isEmpty()) {
                getLog().info("No tags specified. Will not push container image named " + image.getImageName());
            } else {
                exportContainerImages(image, hub);
            }

            getLog().info("Built container image.");
        }
    }

    private void exportContainerImages(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Pushing container images to registry ...");

        // The image configuration cannot produce an empty list of image names.
        for (String imageNameWithTag : image.getImageNames()) {
            String fullImageName = getFullImageNameWithPushRegistry(imageNameWithTag);

            getLog().info("Pushing image: " + fullImageName + " to " + pushRegistry);

            hub.getCommandExecutorService().runCommand(outputDirectory, true, false, PODMAN, PUSH, tlsVerify.getCommand(), fullImageName);
            // Apparently, actually pushing the blobs to a registry causes some output on stderr.

            if (deleteLocalImageAfterPush) {
                getLog().info("Removing image " + fullImageName + " from the local repository");
                hub.getCommandExecutorService().runCommand(outputDirectory, PODMAN, REMOVE_LOCAL, fullImageName);
            }
        }

        getLog().info("All images have been successfully pushed to the registry");
    }
}
