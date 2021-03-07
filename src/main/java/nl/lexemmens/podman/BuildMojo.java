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
        // STEP means a Podman build step
        // --> means the result of a build step. This line contains a hash.
        //
        // The last line always contain the final image hash, which not interesting in this case.
        //
        // A STEP may produce multiline output.

        // The last line (size - 1) contains the image hash. We want the hash produced by the STEP, which is on the second to last line
        int lastLine = processOutput.size() - 2;
        int searchIndex = 0;
        while(searchIndex <= lastLine) {
            String currentStage = null;
            String lastKnownImageHash = null;

            // Read current line
            String currentLine = processOutput.get(searchIndex);

            // Check if the current line defines a new stage
            Matcher stageMatcher = stagePattern.matcher(currentLine);
            boolean currentLineDefinesStage = stageMatcher.find();

            getLog().debug("Processing line: '" + currentLine + "'");
            if(currentLineDefinesStage) {
                currentStage = stageMatcher.group(3);
                getLog().debug("Processing stage in Containerfile: " + currentStage);
            }

            // Find either the next step or image hash - whatever comes first
            int searchStartIndex = searchIndex + 1;
            boolean hashFound = false;
            for(int i = searchStartIndex; i <= lastLine; i++) {
                String candidate = processOutput.get(i);
                getLog().debug("Processing candidate: '" + candidate + "'");

                // Check if the candidate line defines a new stage
                Matcher nextStageMatcher = stagePattern.matcher(candidate);
                boolean candidateLineDefinesStage = nextStageMatcher.find();

                Optional<String> imageHashOptional = retrieveImageHashFromLine(candidate);

                if (candidateLineDefinesStage && !hashFound) {
                    // If we hit this branch, no image hash has been found, thus the current branch has probably no output
                    getLog().info("No hash found for stage '" + currentStage + "'");

                    // Use the index of this line as our next search index
                    searchIndex = i;

                    // Stop searching and continue with the outer loop as we found a new stage.
                    break;
                } else if (candidateLineDefinesStage) {
                    getLog().info("Final image for stage " + currentStage + " is: " + lastKnownImageHash);
                    image.getImageHashPerStage().put(currentStage, lastKnownImageHash);

                    // Use the index of this line as our next search index
                    searchIndex = i;

                    // Stop searching and continue with the outer loop
                    break;
                } else if(imageHashOptional.isPresent()) {
                    // Record the image hash we found
                    lastKnownImageHash = imageHashOptional.get();

                    getLog().info("Found image hash '" + lastKnownImageHash + "' for stage '" + currentStage + "'");
                    hashFound = true;
                } else {
                    getLog().debug("Line contains no stage or image hash: " + candidate);
                }

                if(i == lastLine) {
                    // Last line reached. Ensure we break the outer loop
                    searchIndex = Integer.MAX_VALUE;

                    // Register the image hash we fount last
                    getLog().info("Final image for stage " + currentStage + " is: " + lastKnownImageHash);
                    image.getImageHashPerStage().put(currentStage, lastKnownImageHash);
                }
            }
        }

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
