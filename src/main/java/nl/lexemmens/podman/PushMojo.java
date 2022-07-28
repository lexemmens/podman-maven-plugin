package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

/**
 * PushMojo for pushing container images to a registry/repository
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class PushMojo extends AbstractCatalogSupport {

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

    /**
     * Sets the number of attempts for a Podman push.
     */
    @Parameter(property = "podman.push.retries", defaultValue = "0", required = true)
    int retries;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipPush) {
            getLog().info("Pushing container images is skipped.");
            return;
        }

        checkAuthentication(hub);

        if (pushRegistry == null) {
            String msg = "Failed to push container images. No registry specified. Configure the registry by adding the " +
                    "<pushRegistry><!-- registry --></pushRegistry> tag to your configuration.";

            getLog().error(msg);
            throw new MojoExecutionException(msg);
        }

        getLog().info("Using container-catalog.txt to perform podman push");

        List<String> cataloguedImages = readLocalCatalog();

        if (!cataloguedImages.isEmpty()) {
            pushContainerImages(hub, cataloguedImages);
        }

        getLog().info("All images have been successfully pushed to the registry");
    }

    private void pushContainerImages(ServiceHub hub, List<String> images) throws MojoExecutionException {
        getLog().info("Pushing container images to registry ...");

        for (String fullImage : images) {
            pushImage(hub, fullImage);
        }
    }

    private void pushImage(ServiceHub hub, String fullImageName) throws MojoExecutionException {
        getLog().info("Pushing image: " + fullImageName + " to " + pushRegistry);

        for (int i = 0; i <= retries; i++) {
            try {
                hub.getPodmanExecutorService().push(fullImageName);
                break;
            } catch (MojoExecutionException e) {
                if (i != retries) {
                    getLog().warn("Failed to push image " + fullImageName + ", retrying...");
                } else {
                    throw e;
                }
            }
        }

        if (deleteLocalImageAfterPush) {
            getLog().info("Removing image " + fullImageName + " from the local repository");
            hub.getPodmanExecutorService().removeLocalImage(fullImageName);
        }

        getLog().info("Successfully pushed container image " + fullImageName + " to " + pushRegistry);
    }
}
