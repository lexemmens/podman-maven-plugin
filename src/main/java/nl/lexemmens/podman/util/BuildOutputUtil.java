package nl.lexemmens.podman.util;

import nl.lexemmens.podman.image.ImageConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that helps to determine the image hashes in case of a multiline Containerfile
 */
public final class BuildOutputUtil {

    private static final Pattern IMAGE_HASH_PATTERN = Pattern.compile("\\b([A-Fa-f0-9]{11,64})\\b");

    /**
     * Private class constructor
     */
    private BuildOutputUtil() {
        // Not used
    }

    /**
     * <p>
     * Tries to determine the image hashes from the output of a Podman build command when using multistage
     * containerfiles.
     * </p>
     * <p>
     * This method uses a lookahead strategy to determine the image hashes. It does not care about individual
     * steps but only looks for stages and image hashes. Image hashes can be either 11 or 64 characters in
     * length.
     * </p>
     * <p>
     * Identification starts at a line that defines a stage, i.e. <code>FROM [something] AS stageName</code>. From
     * there it will process every next line until it hits the next stage or reaches the last line, whatever
     * comes first. During reading of lines, the last image hash found will be recorded. When a next stage
     * has been found, the last image hash found will be associated with the stage that was being processed.
     * </p>
     * <p>
     * This method allows STEP definitions in a Containerfile to produce multiline output.
     * </p>
     * <p>
     * This method contains two loops, the outer loop goes from STAGE to STAGE. Inner loop processes each next
     * line in order to determine the hash for a stage
     * </p>
     *
     * @param log           Maven's logger for log output
     * @param image         The image configuration
     * @param processOutput The output of a Podman build command
     */
    public static void determineImageHashes(Log log, ImageConfiguration image, List<String> processOutput) {
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
            String currentStage = null;

            // Read current line
            String currentLine = processOutput.get(searchIndex);

            // Check if the current line defines a new stage
            Matcher stageMatcher = stagePattern.matcher(currentLine);
            boolean currentLineDefinesStage = stageMatcher.find();

            log.debug("Processing line: '" + currentLine + "'");
            if (currentLineDefinesStage) {
                currentStage = stageMatcher.group(3);
                log.debug("Processing stage in Containerfile: " + currentStage);
            }

            // Find either the next step or image hash - whatever comes first
            searchIndex = determineImageHash(log, currentStage, searchIndex + 1, processOutput, image);
        }

        log.debug("Collected hashes: " + image.getImageHashPerStage());
    }

    // Javadoc at private method to provide some context

    /**
     * <p>
     * Tries to determine the image hash for a provided stage by searching the output of a Podman build
     * command from the provided index.
     * </p>
     * <p>
     * This method will process each line and check if it contains either a hash or a new stage definition,
     * whatever comes first.
     * </p>
     * <p>
     * The following steps are taken, in the exact order as listed below:
     * </p>
     * <ul>
     *     <li>
     *         A step does not necessarily produce a hash, but I don't know whether it is possible that a stage results in no hash. It sounds very unlikely, but
     *         just in case we log an warning here. This is probably not something that should happen
     *     </li>
     *     <li>
     *         If a stage has been found *and* a hash has been found, than record the hash for the stage and return the index of the line that contains
     *         the new stage.
     *     </li>
     *     <li>
     *         If a hash has been found, record the hash. The hash may be overwritten later in subsequent iterations.
     *     </li>
     *     <li>
     *         The else section may be the result of multiline output or just output of a step.
     *     </li>
     *     <li>
     *         If we reached the last line, the last hash we found will be associated with the stage that is currently being processed. This will
     *         lead to {@link Integer#MAX_VALUE} being the return value of this method. This should result in the caller of this method to stop
     *         processing any more output.
     *     </li>
     * </ul>
     *
     * @param log           Maven logger
     * @param currentStage  The stage currently being processed
     * @param processOutput The output of the podman build command
     * @param image         The image build configuration
     * @return The index of the line containing the next stage, or {@link Integer#MAX_VALUE} if the last line is reached.
     */
    private static int determineImageHash(Log log, String currentStage, int searchStartIndex, List<String> processOutput, ImageConfiguration image) {
        String lastKnownImageHash = null;

        int lastLine = processOutput.size() - 2;
        int nextIndex = Integer.MAX_VALUE;

        boolean hashFound = false;

        for (int i = searchStartIndex; i <= lastLine; i++) {
            String candidate = processOutput.get(i);
            log.debug("Processing candidate: '" + candidate + "'");

            // Check if the candidate line defines a new stage
            Matcher nextStageMatcher = image.getBuild().getMultistageContainerfilePattern().matcher(candidate);
            boolean candidateLineDefinesStage = nextStageMatcher.find();

            Optional<String> imageHashOptional = retrieveImageHashFromLine(candidate);

            if (candidateLineDefinesStage && !hashFound) {
                // If we hit this branch, no image hash has been found for a stage. This is likely not what you want, but I do not know
                // whether this is
                log.warn("No hash found for stage '" + currentStage + "'! This is likely an error.");

                // Use the index of this line as our next search index
                nextIndex = i;

                // Stop searching and continue with the outer loop as we found a new stage.
                break;
            } else if (candidateLineDefinesStage) {
                log.info("Final image for stage " + currentStage + " is: " + lastKnownImageHash);
                image.getImageHashPerStage().put(currentStage, lastKnownImageHash);

                // Use the index of this line as our next search index
                nextIndex = i;

                // Stop searching and continue with the outer loop
                break;
            } else if (imageHashOptional.isPresent()) {
                // Record the image hash we found
                lastKnownImageHash = imageHashOptional.get();

                log.info("Found image hash '" + lastKnownImageHash + "' for stage '" + currentStage + "'");
                hashFound = true;
            } else {
                log.debug("Line contains no stage or image hash: " + candidate);
            }

            if (i == lastLine) {
                // Register the image hash we fount last
                log.info("Final image for stage " + currentStage + " is: " + lastKnownImageHash);
                image.getImageHashPerStage().put(currentStage, lastKnownImageHash);
            }
        }
        return nextIndex;
    }

    private static Optional<String> retrieveImageHashFromLine(String line) {
        String imageHash = null;
        Matcher matcher = IMAGE_HASH_PATTERN.matcher(line);
        if (matcher.find()) {
            imageHash = matcher.group(1);
        }

        return Optional.ofNullable(imageHash);
    }
}
