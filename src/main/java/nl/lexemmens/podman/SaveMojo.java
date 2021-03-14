package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.StageConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SaveMojo for exporting container images to the file system
 */
@Mojo(name = "save", defaultPhase = LifecyclePhase.NONE)
public class SaveMojo extends AbstractPodmanMojo {

    @Parameter(property = "podman.skip.save", defaultValue = "false")
    boolean skipSave;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipSave) {
            getLog().info("Saving container images is skipped.");
            return;
        }

        checkAuthentication(hub);

        for (SingleImageConfiguration image : allImageConfigurations) {
            if(!image.isValid()) {
                getLog().warn("Skipping save of container image with name " + image.getImageName()
                        + ". Configuration is not valid for this module!");
                continue;
            }

            // No need to check if the image names are empty here - this is checked by the image configuration.
            exportContainerImages(image, hub);
        }
    }

    private void exportContainerImages(SingleImageConfiguration image, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Exporting container images to local disk ...");

        Path targetPodmanDir = Paths.get(image.getBuild().getOutputDirectory().toURI()).resolve(PODMAN_DIRECTORY);
        createTargetFolder(targetPodmanDir);

        if (image.getBuild().isMultistageContainerFile() && image.useCustomImageNameForMultiStageContainerfile()) {
            for (StageConfiguration stage : image.getStages()) {
                for (String imageNameWithTag : image.getImageNamesByStage(stage.getName())) {
                    String fullImageName = getFullImageNameWithPushRegistry(imageNameWithTag);
                    doExportContainerImage(hub, fullImageName, targetPodmanDir);
                }
            }
        } else if (image.getBuild().isMultistageContainerFile()) {
            getLog().warn("Detected multistage Containerfile, but no custom image names have been specified. Falling back to exporting final image.");

            // The image configuration cannot produce an empty list of image names.
            exportContainerImage(image, hub, targetPodmanDir);
        } else {
            // The image configuration cannot produce an empty list of image names.
            exportContainerImage(image, hub, targetPodmanDir);
        }

        getLog().info("Container images exported successfully.");
    }

    private void exportContainerImage(SingleImageConfiguration image, ServiceHub hub, Path targetPodmanDir) throws MojoExecutionException {
        for (String imageName : image.getImageNames()) {
            String fullImageName = getFullImageNameWithPushRegistry(imageName);

            String archiveName = String.format("%s.tar.gz", normaliseImageName(fullImageName));

            getLog().info("Exporting image " + fullImageName + " to " + targetPodmanDir + "/" + archiveName);
            hub.getPodmanExecutorService().save(archiveName, fullImageName);
        }
    }

    private void doExportContainerImage(ServiceHub hub, String fullImageName, Path targetPodmanDir) throws MojoExecutionException {
        String archiveName = String.format("%s.tar.gz", normaliseImageName(fullImageName));

        getLog().info("Exporting image " + fullImageName + " to " + targetPodmanDir + "/" + archiveName);
        hub.getPodmanExecutorService().save(archiveName, fullImageName);
    }

    private void createTargetFolder(Path targetPodmanDir) throws MojoExecutionException {
        try {
            // Does not fail if folder already exists
            Files.createDirectories(targetPodmanDir);
        } catch (IOException e) {
            String msg = "Failed to create directory '" + targetPodmanDir + "'. An IOException occurred: " + e.getMessage();
            getLog().error(msg, e);
            throw new MojoExecutionException(msg, e);
        }
    }

    private String normaliseImageName(String fullImageName) {
        String[] imageNameParts = fullImageName.split("\\/");
        String tagAndVersion = imageNameParts[imageNameParts.length - 1];
        return tagAndVersion.replaceAll("[\\.\\/\\-\\*:]", "_");
    }
}
