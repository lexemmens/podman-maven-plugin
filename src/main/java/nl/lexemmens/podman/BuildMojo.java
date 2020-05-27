package nl.lexemmens.podman;

import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * BuildMojo for building container images using Podman
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.INSTALL)
public class BuildMojo extends AbstractPodmanMojo {

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(property = "podman.skip.build", defaultValue = "false")
    boolean skipBuild;

    /**
     * Indicates if tagging container images should be skipped
     */
    @Parameter(property = "podman.skip.tag", defaultValue = "false")
    boolean skipTag;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipBuild) {
            getLog().info("Building container images is skipped.");
            return;
        }

        checkAuthentication(hub);

        for(ImageConfiguration image : images) {
            decorateDockerFile(image, hub);
            buildContainerImage(image, hub);
            tagContainerImage(image, hub);

            getLog().info("Built container image.");
        }
    }

    private void decorateDockerFile(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().debug("Filtering Dockerfile...");
        hub.getDockerfileDecorator().decorateDockerfile(image);
    }

    private void buildContainerImage(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Building container image...");

        String imageHash = hub.getPodmanExecutorService().build(image);
        image.setImageHash(imageHash);
    }

    private void tagContainerImage(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        if (skipTag) {
            getLog().info("Tagging container images is skipped.");
            return;
        }

        if (image.getBuild().getAllTags().isEmpty()) {
            getLog().info("No tags specified. Skipping tagging of container images.");
            return;
        }

        if (image.getImageHash().isPresent()) {
            String imageHash = image.getImageHash().get();
            for (String imageNameWithTag : image.getImageNames()) {
                String fullImageName = getFullImageNameWithPushRegistry(imageNameWithTag);

                getLog().info("Tagging container image " + imageHash + " as " + fullImageName);

                hub.getPodmanExecutorService().tag(imageHash, fullImageName);
            }
        } else {
            getLog().info("No image hash available. Skipping tagging container image.");
        }
    }




}
