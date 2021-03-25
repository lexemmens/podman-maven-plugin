package nl.lexemmens.podman.helper;

import nl.lexemmens.podman.config.image.StageConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to format image names.
 * <p>
 * Supported formatter options:
 * <ul>
 *     <li>%a: Results in the artifactId (sanitized)</li>
 *     <li>%d: Results in the directory that contains the Containerfile</li>
 *     <li>%g: Results in the last part of the groupId (after the last .)</li>
 *     <li>%n: Results in a number (starting at 0)</li>
 *     <li>%l: Results in 'latest' in case the current version is a SNAPSHOT. Otherwise version of the project</li>
 *     <li>%t: Results in 'snapshot-[timestamp]'</li>
 *     <li>%v: Results in the version of the project</li>
 * </ul>
 */
public class ImageNameHelper {

    private final MavenProject mavenProject;
    private final ParameterReplacer parameterReplacer;

    /**
     * Constructs a new instance of this {@link ImageNameHelper}
     *
     * @param mavenProject The MavenProject to use
     */
    public ImageNameHelper(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        this.parameterReplacer = new ParameterReplacer(initReplacements());
    }

    /**
     * Adapt some replacements based on the new {@link SingleImageConfiguration}.
     * <p>
     * Some replacements may be based on some properties from the {@link SingleImageConfiguration}. This method
     * ensures that they are all adapted and that the call to {@link #formatImageName(SingleImageConfiguration)} returns
     * the correct value.
     *
     * @param imageConfiguration The {@link SingleImageConfiguration} to use
     */
    public void adaptReplacemeents(SingleImageConfiguration imageConfiguration) {
        parameterReplacer.adaptReplacements(imageConfiguration);
    }

    /**
     * Formats the imageName according to the specifications from this class. Both image names as specified
     * in the 'name' tag and the 'imageName' tag (part of the stage section) are being proessed.
     *
     * @param imageConfiguration The imageConfiguration containing the image names to format.
     */
    public void formatImageName(SingleImageConfiguration imageConfiguration) {
        String imageName = parameterReplacer.replace(imageConfiguration.getImageName());
        imageConfiguration.setImageName(imageName);

        if (imageConfiguration.getStages() != null && imageConfiguration.getStages().length > 0) {
            for (StageConfiguration stage : imageConfiguration.getStages()) {
                String stageImageName = parameterReplacer.replace(stage.getImageName());
                stage.setImageName(stageImageName);
            }
        }
    }

    private Map<String, ParameterReplacer.Replacement> initReplacements() {
        final Map<String, ParameterReplacer.Replacement> replacements = new HashMap<>();
        replacements.put("a", new ArtifactIdReplacement());
        replacements.put("d", new ContainerFileDirectoryReplacement());
        replacements.put("g", new GroupIdReplacement());
        replacements.put("n", new ImageNumberReplacement());
        replacements.put("l", new SnapshotLatestReplacement());
        replacements.put("t", new SnapshotTimestampReplacement());
        replacements.put("v", new ProjectVersionReplacement());

        return replacements;
    }

    /**
     * Replacement that replaces a preconfigured character with the artifactId of the Maven Project.
     */
    private class ArtifactIdReplacement implements ParameterReplacer.Replacement {

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            return mavenProject.getArtifactId();
        }
    }

    /**
     * Replacement that replaces a preconfigured character with the version of the Maven Project.
     */
    private class ProjectVersionReplacement implements ParameterReplacer.Replacement {

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            return alignWithNamingConvention(mavenProject.getVersion());
        }
    }

    /**
     * Replacement that replaces a preconfigured character with 'latest' when the project's
     * version ends with '-SNAPSHOT'. Otherwise it will return the version of the project.
     */
    private class SnapshotLatestReplacement implements ParameterReplacer.Replacement {

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            String version = mavenProject.getVersion();
            if (version.endsWith("-SNAPSHOT")) {
                version = "latest";
            }
            return alignWithNamingConvention(version);
        }
    }

    /**
     * Replacement that replaces a preconfigured character with last part of the
     * Maven groupId.
     */
    private class GroupIdReplacement implements ParameterReplacer.Replacement {

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            String groupId = mavenProject.getGroupId();
            int idx = groupId.lastIndexOf('.');
            if (idx != -1) {
                groupId = groupId.substring(idx + 1);
            }
            return alignWithNamingConvention(groupId);
        }
    }

    /**
     * Replacement that replaces a preconfigured character with 'snapshot-[timestamp]'. The
     * timestamp will be in format 'yyMMdd-HHmmss-SSSS'.
     */
    private static class SnapshotTimestampReplacement implements ParameterReplacer.Replacement {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd-HHmmss-SSSS");

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            Calendar now = Calendar.getInstance();
            dateFormat.setTimeZone(now.getTimeZone());
            return alignWithNamingConvention("snapshot-" + dateFormat.format(now.getTime()));
        }
    }

    /**
     * Replacement that replaces a preconfigured character with the name of the directory
     * the Containerfile is in.
     */
    private static class ContainerFileDirectoryReplacement implements ParameterReplacer.Replacement {

        Path containerFileDirectory;

        @Override
        public void adaptReplacement(SingleImageConfiguration image) {
            this.containerFileDirectory = image.getBuild().getSourceContainerFileDir();
        }

        @Override
        public String get() {
            // /someDirectory/subFolder/Containerfile => subFolder
            return alignWithNamingConvention(containerFileDirectory.getParent().getFileName().toString());
        }
    }

    /**
     * Replacement that replaces a preconfigured character with a zero based integer.
     */
    private static class ImageNumberReplacement implements ParameterReplacer.Replacement {

        AtomicInteger imageNumber = new AtomicInteger(0);

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            return alignWithNamingConvention(String.format("%s", imageNumber.getAndIncrement()));
        }
    }

    /**
     * As per Docker's naming conventions, image names must meet the following criteria:
     * <ul>
     * <li>Name components may contain lowercase letters, digits and separators.</li>
     * <li>A separator is defined as:
     * <ul>
     *     <li>a period</li>
     *     <li>one or two underscores</li>
     *     <li>one or more dashes</li>
     * </ul>
     * </li>
     * <li>A name component may not start or end with a separator.</li>
     * </ul>
     *
     * @param imageName The image name to
     * @return The image name, meeting the image naming conventions
     * @see <a href="https://docs.docker.com/engine/reference/commandline/tag/">Docker docs</a>
     */
    private static String alignWithNamingConvention(String imageName) {
        StringBuilder ret = new StringBuilder();
        int underscores = 0;
        boolean lastWasADot = false;
        for (char character : imageName.toCharArray()) {
            if (character == '_') {
                underscores++;

                // Max 2 underscores after eachother
                if (underscores <= 2) {
                    ret.append(character);
                }

                continue;
            }

            if (character == '.') {
                // Only one dot is allowed
                if (!lastWasADot) {
                    ret.append(character);
                }
                lastWasADot = true;
                continue;
            }

            underscores = 0;
            lastWasADot = false;
            if (Character.isLetter(character) || Character.isDigit(character) || character == '-') {
                ret.append(character);
            }
        }

        // All characters must be lowercase
        return ret.toString().toLowerCase();
    }

}
