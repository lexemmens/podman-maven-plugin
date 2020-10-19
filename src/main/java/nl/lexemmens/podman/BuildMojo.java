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

        for(ImageConfiguration image : images) {
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

        if(image.getBuild().isMultistageContainerFile()) {
            getLog().info("Detected multistage Containerfile...");
            determineImageHashes(image, processOutput, finalImageHash);
        }
    }

    private void determineImageHashes(ImageConfiguration image, List<String> processOutput, String finalImageHash) {
        // Use size -2 as the last line is the image hash of the final image, which we already captured before.
        Pattern pattern = image.getBuild().getMultistageContainerfilePattern();
        getLog().debug("Using regular expression: " + pattern);
        boolean firstOccurrenceFound = false;

        String currentStage = null;
        for(int i = 0; i <= processOutput.size() - 2; i++) {
            String currentLine = processOutput.get(i);
            Matcher matcher = pattern.matcher(currentLine);
            boolean matches = matcher.find();

            getLog().debug("Processing line: '" + currentLine + "', matches: " + matches);

            if(matches) {
                boolean isFirstStage = currentStage == null;

                if(isFirstStage) {
                    currentStage = matcher.group(3);

                    getLog().debug("Initial detection of a stage in Containerfile. Stage: " + currentStage);
                } else {
                    // If we have found a new stage, it means we reached the end of the previous stage. Thus the hash corresponding to the
                    // must be on the previous line.
                    String lineContainingImageHash = processOutput.get(i - 1);
                    extractAndSaveImageHashFromLine(image, currentStage, lineContainingImageHash);

                    // Save the current stage
                    currentStage = matcher.group(3);
                    getLog().debug("Found new stage in Containerfile: " + currentStage);
                }
            } else if (i == processOutput.size() - 2) {
                getLog().info("Using image hash of final image (" + finalImageHash + ") for stage: " + currentStage);
                image.getImageHashPerStage().put(currentStage, finalImageHash);
            }
        }

        getLog().debug("Collected hashes: " + image.getImageHashPerStage());
    }

    private void extractAndSaveImageHashFromLine(ImageConfiguration image, String currentStage, String lineContainingImageHash) {
        Optional<String> imageHash = retrieveImageHashFromLine(lineContainingImageHash);
        if (imageHash.isPresent()) {
            getLog().info("Found image hash " + imageHash.get() + " for stage " + currentStage);
            image.getImageHashPerStage().put(currentStage, imageHash.get());
        } else {
            getLog().warn("Failed to determine image hash for stage " + currentStage);
        }
    }

    private Optional<String> retrieveImageHashFromLine(String line) {
        String imageHash = null;
        Matcher matcher = IMAGE_HASH_PATTERN.matcher(line);
        if(matcher.find()) {
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

        if(image.getBuild().isMultistageContainerFile() && image.useCustomImageNameForMultiStageContainerfile()) {
            for(Map.Entry<String, String> stageImage : image.getImageHashPerStage().entrySet()) {
                for(String imageName : image.getImageNamesByStage(stageImage.getKey())) {
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
