package nl.lexemmens.podman;

import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;
import java.util.Map;

import static nl.lexemmens.podman.util.BuildOutputUtil.determineImageHashes;

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

        for (ImageConfiguration image : images) {
            if (!image.isValid()) {
                getLog().warn("Skipping build of container image with name " + image.getImageName()
                        + ". Configuration is not valid for this module!");
                continue;
            }

            decorateContainerfile(image, hub);
            buildContainerImage(image, hub);
            tagContainerImage(image, hub);

            getLog().info("Built container image.");
        }
    }

    private void decorateContainerfile(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Filtering Containerfile...");
        hub.getContainerfileDecorator().decorateContainerfile(image);
    }

    private void buildContainerImage(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Building container image...");

        List<String> processOutput = hub.getPodmanExecutorService().build(image);

        // Read the final image hash
        String finalImageHash = processOutput.get(processOutput.size() - 1);
        getLog().debug("Determined final image hash as " + finalImageHash);
        image.setFinalImageHash(finalImageHash);

        if (image.getBuild().isMultistageContainerFile()) {
            getLog().info("Detected multistage Containerfile...");
            determineImageHashes(getLog(), image, processOutput);
        }
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

        if (image.getBuild().isMultistageContainerFile() && image.useCustomImageNameForMultiStageContainerfile()) {
            for (Map.Entry<String, String> stageImage : image.getImageHashPerStage().entrySet()) {
                for (String imageName : image.getImageNamesByStage(stageImage.getKey())) {
                    String fullImageName = getFullImageNameWithPushRegistry(imageName);

                    getLog().info("Tagging container image " + stageImage.getValue() + " from stage " + stageImage.getKey() + " as " + fullImageName);

                    hub.getPodmanExecutorService().tag(stageImage.getValue(), fullImageName);
                }
            }
        } else if (image.getBuild().isMultistageContainerFile()) {
            getLog().warn("Missing container names for multistage Containerfile. Falling back to tagging the final container image.");
            tagFinalImage(image, hub);
        } else {
            tagFinalImage(image, hub);
        }
    }

    private void tagFinalImage(ImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        if (image.getFinalImageHash().isPresent()) {
            String imageHash = image.getFinalImageHash().get();
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
