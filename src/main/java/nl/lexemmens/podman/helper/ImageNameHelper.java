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

public class ImageNameHelper {

    private final MavenProject mavenProject;
    private final ParameterReplacer parameterReplacer;

    public ImageNameHelper(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        this.parameterReplacer = new ParameterReplacer(initReplacements());
    }

    public void adaptReplacemeents(SingleImageConfiguration imageConfiguration) {
        parameterReplacer.adaptReplacements(imageConfiguration);
    }

    public void formatImageName(SingleImageConfiguration imageConfiguration) {
        String imageName = parameterReplacer.replace(imageConfiguration.getImageName());
        imageConfiguration.setImageName(imageName);

        if(imageConfiguration.getStages() != null && imageConfiguration.getStages().length > 0) {
            for(StageConfiguration stage : imageConfiguration.getStages()) {
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

    private class ProjectVersionReplacement implements ParameterReplacer.Replacement {

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            return mavenProject.getVersion();
        }
    }

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
            return version;
        }
    }

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
                groupId = groupId.substring(idx, groupId.length() - 1);
            }
            return groupId;
        }
    }

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
            return "snapshot-" + dateFormat.format(now.getTime());
        }
    }

    private static class ContainerFileDirectoryReplacement implements ParameterReplacer.Replacement {

        Path containerFileDirectory;

        @Override
        public void adaptReplacement(SingleImageConfiguration image) {
            this.containerFileDirectory = image.getBuild().getSourceContainerFileDir();
        }

        @Override
        public String get() {
            // /someDirectory/subFolder/Containerfile => subFolder
            return containerFileDirectory.getParent().getFileName().toString();
        }
    }

    private static class ImageNumberReplacement implements ParameterReplacer.Replacement {

        AtomicInteger imageNumber = new AtomicInteger(0);

        @Override
        public void adaptReplacement(SingleImageConfiguration notUsed) {
            // Ignore
        }

        @Override
        public String get() {
            return String.format("%s", imageNumber.getAndIncrement());
        }
    }

}
