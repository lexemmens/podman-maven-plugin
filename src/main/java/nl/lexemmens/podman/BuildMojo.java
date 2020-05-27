package nl.lexemmens.podman;

import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

/**
 * BuildMojo for building container images using Podman
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.INSTALL)
public class BuildMojo extends AbstractPodmanMojo {

    private static final String TAG = "tag";
    private static final String BUILD_CMD = "build";
    private static final String DOCKERFILE_CMD = "--file=";
    public static final String NO_CACHE_CMD = "--no-cache=";

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

        List<String> processOutput = hub.getCommandExecutorService().runCommand(image.getBuild().getOutputDirectory(), true, false,
                PODMAN,
                BUILD_CMD,
                DOCKERFILE_CMD + image.getBuild().getTargetDockerfile(),
                NO_CACHE_CMD + image.getBuild().isNoCache(),
                tlsVerify.getCommand(),
                ".");

        image.setImageHash(processOutput.get(processOutput.size() - 1));
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

                // Ignore output
                hub.getCommandExecutorService().runCommand(image.getBuild().getOutputDirectory(), PODMAN, TAG, imageHash, fullImageName);
            }
        } else {
            getLog().info("No image hash available. Skipping tagging container image.");
        }
    }




}
