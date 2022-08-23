package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.helper.MultiStageBuildOutputHelper;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * BuildMojo for building container images using Podman
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.INSTALL)
public class BuildMojo extends AbstractPodmanMojo {

    private final MultiStageBuildOutputHelper buildOutputHelper;
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
    /**
     * Indicates if attaching the container-catalog.txt file to the build should be skipped.
     */
    @Parameter(property = "podman.skip.catalog", defaultValue = "false")
    boolean skipCatalog;

    /**
     * Constructor
     */
    public BuildMojo() {
        super();

        this.buildOutputHelper = new MultiStageBuildOutputHelper();
    }

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        checkAuthentication(hub);

        for (SingleImageConfiguration image : resolvedImages) {
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

        catalogContainers(resolvedImages, hub);
    }

    @Override
    protected boolean skipGoal() {
        return skipBuild;
    }

    private void decorateContainerfile(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Filtering Containerfile...");
        hub.getContainerfileDecorator().decorateContainerfile(image);
    }

    private void buildContainerImage(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Building container image...");

        List<String> processOutput = hub.getPodmanExecutorService().build(image);

        // Read the final image hash
        String finalImageHash = processOutput.get(processOutput.size() - 1);
        getLog().debug("Determined final image hash as " + finalImageHash);
        image.setFinalImageHash(finalImageHash);

        if (image.getBuild().isMultistageContainerFile()) {
            getLog().info("Detected multistage Containerfile...");
            buildOutputHelper.recordImageHashes(getLog(), image, processOutput);
        }
    }

    private void tagContainerImage(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        if (skipTag) {
            getLog().info("Tagging container images is skipped.");
            return;
        }

        if (image.getBuild().getAllTags().isEmpty()) {
            getLog().info("No tags specified. Skipping tagging of container images.");
            return;
        }

        if (image.getBuild().isMultistageContainerFile() && image.useCustomImageNameForMultiStageContainerfile()) {
            tagImagesOfMultiStageContainerfile(image, hub);
        } else if (image.getBuild().isMultistageContainerFile()) {
            getLog().warn("Missing container names for multistage Containerfile. Falling back to tagging the final container image.");
            tagFinalImage(image, hub);
        } else {
            tagFinalImage(image, hub);
        }
    }

    private void tagImagesOfMultiStageContainerfile(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        for (Map.Entry<String, String> stageImage : image.getImageHashPerStage().entrySet()) {
            List<String> imageNamesByStage = image.getImageNamesByStage(stageImage.getKey());

            if (imageNamesByStage.isEmpty()) {
                getLog().warn("No image name configured for build stage: " + stageImage.getKey() + ". Image " + stageImage.getValue() + " not tagged!");
            } else {
                for (String imageName : imageNamesByStage) {
                    String fullImageName = getFullImageNameWithPushRegistry(imageName);

                    getLog().info("Tagging container image " + stageImage.getValue() + " from stage " + stageImage.getKey() + " as " + fullImageName);

                    hub.getPodmanExecutorService().tag(stageImage.getValue(), fullImageName);
                }
            }
        }
    }

    private void tagFinalImage(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
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

    private void catalogContainers(List<SingleImageConfiguration> images, ServiceHub hub) throws MojoExecutionException {
        List<String> containerCatalog = getContainerCatalog(images);
        if (containerCatalog.isEmpty()) {
            getLog().info("No containers were catalogued.");
            return;
        }

        containerCatalog.add(0, CATALOG_HEADER);

        String catalogFileName = String.format("%s.txt", CATALOG_ARTIFACT_NAME);
        Path catalogPath = Paths.get(project.getBuild().getDirectory(), catalogFileName);
        try {
            Files.write(catalogPath, containerCatalog);
        } catch (IOException e) {
            getLog().error("Failed to write catalog file! Caught: " + e.getMessage());
            throw new MojoExecutionException(e.getMessage(), e);
        }

        if (skipCatalog) {
            getLog().info("Skipping attaching of catalog artifact.");
        } else {
            getLog().info("Attaching catalog artifact: " + catalogPath);

            hub.getMavenProjectHelper().attachArtifact(project, "txt", CATALOG_ARTIFACT_NAME, catalogPath.toFile());
        }
    }

    private List<String> getContainerCatalog(List<SingleImageConfiguration> images) {
        return images.stream()
                .map(this::singleImageConfigurationToFullImageList)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
