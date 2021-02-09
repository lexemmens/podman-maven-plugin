package nl.lexemmens.podman;

import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BuildMojo for building container images using Podman
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.INSTALL)
public class BuildMojo extends AbstractPodmanMojo {

    private static final Pattern IMAGE_HASH_PATTERN = Pattern.compile("\\b([A-Fa-f0-9]{11,64})\\b");

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
            determineImageHashes(image, processOutput);
        }
    }

    private void determineImageHashes(ImageConfiguration image, List<String> processOutput) {
        // Use size -2 as the last line is the image hash of the final image, which we already captured before.
        Pattern stagePattern = image.getBuild().getMultistageContainerfilePattern();
        getLog().debug("Using regular expression: " + stagePattern);

        // We are interested in output that starts either with:
        // * STEP
        // * -->
        //
        // STEP means a build step
        // --> means the result of a build step
        //
        // The last line always contain the final image hash, which not interesting in this case.


        String currentStage = null;
        String lastKnownImageHash = null;
        for (int i = 0; i <= processOutput.size() - 2; i++) {
            // Read current line and next line (this is safe as we only loop until size - 2)
            String currentLine = processOutput.get(i);
            String nextLine = processOutput.get(i + 1);

            // Check if the current line defines a new stage
            Matcher stageMatcher = stagePattern.matcher(currentLine);
            boolean currentLineDefinesStage = stageMatcher.find();

            getLog().debug("Processing line: '" + currentLine + "', matches: " + currentLineDefinesStage);

            // Check if the next line contains a hash
            Optional<String> imageHashOptional = retrieveImageHashFromLine(nextLine);

            if(currentLineDefinesStage) {
                boolean isFirstStage = currentStage == null;

                if(!isFirstStage) {
                    // If it is not the first stage, then we must save the image hash of the previous stage
                    getLog().info("Final image for stage " + currentStage + " is: " + lastKnownImageHash);
                    image.getImageHashPerStage().put(currentStage, lastKnownImageHash);
                }

                currentStage = stageMatcher.group(3);
                lastKnownImageHash = null;

                getLog().debug("Found stage in Containerfile: " + currentStage);
            } else if(currentLine.startsWith("STEP") && imageHashOptional.isPresent()) {
                lastKnownImageHash = imageHashOptional.get();
                getLog().debug("Stage " + currentStage + ", current image hash: " + lastKnownImageHash);
            } else {
                getLog().debug("Not a (valid) step output, continuing...");
            }
        }

        // Save the last image hash we know
        getLog().info("Final image for stage " + currentStage + " is: " + lastKnownImageHash);
        image.getImageHashPerStage().put(currentStage, lastKnownImageHash);

        getLog().debug("Collected hashes: " + image.getImageHashPerStage());
    }


    private Optional<String> retrieveImageHashFromLine(String line) {
        String imageHash = null;
        Matcher matcher = IMAGE_HASH_PATTERN.matcher(line);
        if (matcher.find()) {
            imageHash = matcher.group(1);
        }

        return Optional.ofNullable(imageHash);
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
