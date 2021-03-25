package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.StageConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
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


        for (SingleImageConfiguration image : resolvedImages) {
            if (!image.isValid()) {
                getLog().warn("Skipping push of container image with name " + image.getImageName()
                        + ". Configuration is not valid for this module!");
                continue;
            }

            if (image.getBuild().getAllTags().isEmpty()) {
                getLog().info("No tags specified. Will not push container image named " + image.getImageName());
            } else {
                pushContainerImages(image, hub);
            }
        }

        getLog().info("All images have been successfully pushed to the registry");
    }

    private void pushContainerImages(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Pushing container images to registry ...");

        if (image.getBuild().isMultistageContainerFile() && image.useCustomImageNameForMultiStageContainerfile()) {
            for (StageConfiguration stage : image.getStages()) {
                for (String imageNameWithTag : image.getImageNamesByStage(stage.getName())) {
                    String fullImageName = getFullImageNameWithPushRegistry(imageNameWithTag);
                    doPushContainerImage(hub, fullImageName);
                }
            }
        } else if (image.getBuild().isMultistageContainerFile()) {
            getLog().warn("Detected multistage Containerfile, but no custom image names have been specified. Falling back to pushing final image.");

            // The image configuration cannot produce an empty list of image names.
            pushRegularContainerImage(image, hub);
        } else {
            // The image configuration cannot produce an empty list of image names.
            pushRegularContainerImage(image, hub);
        }
    }

    private void pushRegularContainerImage(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        for (String imageNameWithTag : image.getImageNames()) {
            String fullImageName = getFullImageNameWithPushRegistry(imageNameWithTag);
            doPushContainerImage(hub, fullImageName);
        }
    }

    private void doPushContainerImage(ServiceHub hub, String fullImageName) throws MojoExecutionException {
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
