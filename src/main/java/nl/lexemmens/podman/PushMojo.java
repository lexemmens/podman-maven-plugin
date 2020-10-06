package nl.lexemmens.podman;

import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.image.StageConfiguration;
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

        for (ImageConfiguration image : images) {
            if (image.getBuild().getAllTags().isEmpty()) {
                getLog().info("No tags specified. Will not push container image named " + image.getImageName());
            } else {
                pushContainerImages(image, hub);
            }
        }

        getLog().info("All images have been successfully pushed to the registry");
    }

    private void pushContainerImages(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Pushing container images to registry ...");

        if(image.getBuild().isMultistageContainerFile() && image.isCustomImageNameForMultiStageContainerfile()) {
            for (StageConfiguration stage : image.getStages()) {
                for(String imageNameWithTag : image.getImageNamesByStage(stage.getName())) {
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

    private void pushRegularContainerImage(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        for (String imageNameWithTag : image.getImageNames()) {
            String fullImageName = getFullImageNameWithPushRegistry(imageNameWithTag);
            doPushContainerImage(hub, fullImageName);
        }
    }

    private void doPushContainerImage(ServiceHub hub, String fullImageName) throws MojoExecutionException {
        getLog().info("Pushing image: " + fullImageName + " to " + pushRegistry);

        hub.getPodmanExecutorService().push(fullImageName);

        if (deleteLocalImageAfterPush) {
            getLog().info("Removing image " + fullImageName + " from the local repository");
            hub.getPodmanExecutorService().removeLocalImage(fullImageName);
        }

        getLog().info("Successfully pushed container image " + fullImageName + " to " + pushRegistry);
    }
}
