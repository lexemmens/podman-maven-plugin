package nl.lexemmens.podman.helper;

import nl.lexemmens.podman.image.ImageConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that helps to determine the image hashes in case of a multiline Containerfile
 */
public final class MultiStageBuildOutputHelper {

    private static final Pattern IMAGE_HASH_PATTERN = Pattern.compile("\\b([A-Fa-f0-9]{11,64})\\b");

    /**
     * <p>
     * Tries to determine the image hashes from the output of a Podman build command when using multistage
     * containerfiles.
     * </p>
     * <p>
     * This method uses a lookahead strategy to determine the image hashes. It does not care about individual
     * steps but only looks for stages and image hashes. Image hashes must be either 11 or 64 characters in
     * length in order to be detected.
     * </p>
     * <p>
     * Identification starts at a line that defines a stage, i.e. <code>FROM [something] AS stageName</code>. From
     * there it will process every next line until it hits the next stage or reaches the last line, whatever
     * comes first. During reading of lines, the last image hash found will be recorded. When a next stage
     * has been found, the last image hash found will be associated with the stage that was being processed.
     * </p>
     * <p>
     * To be more specific, the following steps are taken by this method, in the order as listed below:
     * </p>
     * <ul>
     *     <li>
     *         Read the first line of the output
     *     </li>
     *     <li>
     *         Retrieve the current stage from the the line. If the stage is <code>null</code>, then continue
     *         with the next line
     *     </li>
     *     <li>
     *         If a stage has been found, find the image hash
     *     </li>
     *     <li>
     *         Record the image hash for the current stage
     *     </li>
     * </ul>
     * <p>
     * This method allows STEP definitions in a Containerfile to produce multiline output.
     * </p>
     *
     * @param log           Maven's logger for log output
     * @param image         The image configuration
     * @param processOutput The output of a Podman build command
     */
    public void recordImageHashes(Log log, ImageConfiguration image, List<String> processOutput) {
        // Use size -2 as the last line is the image hash of the final image, which we already captured before.
        Pattern stagePattern = image.getBuild().getMultistageContainerfilePattern();
        log.debug("Using regular expression: " + stagePattern);

        // We are interested in output that starts either with:
        // * STEP
        // * -->
        //
        // STEP means a Podman build step
        // --> means the result of a build step. This line contains a hash (post 1.x)
        //
        // The last line always contain the final image hash, which not interesting in this case.
        //
        // A STEP may produce multiline output.

        // The last line (size - 1) contains the image hash. We want the hash produced by the STEP, which is on the second to last line
        int lastLine = processOutput.size() - 2;
        int searchIndex = 0;
        while (searchIndex <= lastLine) {
            String currentStage;

            // Read current line
            String currentLine = processOutput.get(searchIndex);

            // Determine the current stage
            currentStage = getCurrentStage(log, stagePattern, currentLine);
            if(currentStage == null) {
                searchIndex++;
                continue;
            }

            // Find the corresponding image hash
            ImageHashSearchResult imageHashSearchResult = findImageHash(log, processOutput, searchIndex + 1, stagePattern);

            // Save it
            recordImageHash(log, image, currentStage, imageHashSearchResult);

            // Continue with next iteration or break the loop if we have reached the last line
            if (imageHashSearchResult.isLastLine) {
                break;
            } else {
                searchIndex = imageHashSearchResult.nextIndex;
            }
        }

        log.debug("Collected hashes: " + image.getImageHashPerStage());
    }

    private static void recordImageHash(Log log, ImageConfiguration image, String currentStage, ImageHashSearchResult imageHashSearchResult) {
        if (imageHashSearchResult.imageHash == null) {
            log.warn("No image hash found for stage: '" + currentStage + "'");
        } else {
            log.info("Final image for stage " + currentStage + " is: " + imageHashSearchResult.imageHash);
            image.getImageHashPerStage().put(currentStage, imageHashSearchResult.imageHash);
        }
    }

    private static String getCurrentStage(Log log, Pattern stagePattern, String currentLine) {
        String currentStage = null;

        // Check if the current line defines a new stage
        Matcher stageMatcher = stagePattern.matcher(currentLine);
        boolean currentLineDefinesStage = stageMatcher.find();

        log.debug("Processing line: '" + currentLine + "'");
        if (currentLineDefinesStage) {
            currentStage = stageMatcher.group(3);
            log.debug("Processing stage in Containerfile: " + currentStage);
        }
        return currentStage;
    }

    // Javadoc at private method to provide some context

    /**
     * <p>
     * Tries to determine the image hash for a provided stage by searching the output of a Podman build
     * command from the provided index. Returns a {@link ImageHashSearchResult}.
     * </p>
     * <p>
     * This method will process each line, starting from a given inex, and check if it contains either a
     * hash or a new stage definition, whatever comes first.
     * </p>
     * <p>
     * The search result maybe one of the following:
     * </p>
     * <ul>
     *     <li>
     *         A step does not necessarily produce a hash. I don't know whether it is possible that a stage results in no hash, but it is a case that is at
     *         least covered. In this case the hash in the {@link ImageHashSearchResult} will be <code>null</code>
     *     </li>
     *     <li>
     *         If a stage has been found, an instance of {@link ImageHashSearchResult} will be returned. It a hash was found, it contains this hash. The
     *         {@link ImageHashSearchResult} also contains the index of the next stage or -1 if the last line was reached. In the latter case, the <code>lastLine</code>
     *         identifier in the {@link ImageHashSearchResult} has the value <coe>true</coe>
     *     </li>
     * </ul>
     *
     * @param log               Maven logger
     * @param processOutput     The output of the podman build command
     * @param searchStartIndex  The index of the output from where the search to start
     * @param multiStagePattern The pattern to use to recognise a new stage in a containerfile.
     * @return An instance of {@link ImageHashSearchResult} containing the hash (if found) and the index of the next stage. It also has an identifier that can be used
     * to detect if the last row of the build output has been reached.
     */
    private static ImageHashSearchResult findImageHash(Log log, List<String> processOutput, int searchStartIndex, Pattern multiStagePattern) {
        ImageHashSearchResult searchResult = ImageHashSearchResult.EMPTY;
        String lastKnownImageHash = null;

        int lastLine = processOutput.size() - 2;

        for (int idx = searchStartIndex; idx <= lastLine; idx++) {
            boolean isLastLine = idx == lastLine;

            String candidate = processOutput.get(idx);
            log.debug("Processing candidate: '" + candidate + "'");

            // Check if the candidate line defines a new stage
            Matcher nextStageMatcher = multiStagePattern.matcher(candidate);
            boolean candidateLineDefinesStage = nextStageMatcher.find();

            Optional<String> imageHashOptional = retrieveImageHashFromLine(candidate);
            if (!candidateLineDefinesStage && imageHashOptional.isPresent()) {
                // Record the image hash we found
                lastKnownImageHash = imageHashOptional.get();
                log.debug("Derived hash: '" + lastKnownImageHash + "' from:     " + candidate);
            }

            if (candidateLineDefinesStage || isLastLine) {
                searchResult = new ImageHashSearchResult(lastKnownImageHash, idx, isLastLine);

                // Stop searching and continue with the outer loop as we found a new stage.
                break;
            } else {
                log.debug("No stage or image hash on line: " + candidate);
            }
        }

        return searchResult;
    }

    private static Optional<String> retrieveImageHashFromLine(String line) {
        String imageHash = null;
        Matcher matcher = IMAGE_HASH_PATTERN.matcher(line);
        if (matcher.find()) {
            imageHash = matcher.group(1);
        }

        return Optional.ofNullable(imageHash);
    }

    private static class ImageHashSearchResult {

        private static final ImageHashSearchResult EMPTY = new ImageHashSearchResult(null, -1, true);

        private final String imageHash;
        private final int nextIndex;
        private final boolean isLastLine;

        public ImageHashSearchResult(String imageHash, int nextIndex, boolean isLastLine) {
            this.imageHash = imageHash;
            this.nextIndex = nextIndex;
            this.isLastLine = isLastLine;
        }
    }
}
