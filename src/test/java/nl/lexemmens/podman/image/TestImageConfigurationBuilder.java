package nl.lexemmens.podman.image;

import nl.lexemmens.podman.enumeration.ContainerFormat;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestImageConfigurationBuilder {

    private final ImageConfiguration image;

    public TestImageConfigurationBuilder(String name) {
        image = new ImageConfiguration();
        image.name = name;
        image.build = new BuildImageConfiguration();
    }

    public TestImageConfigurationBuilder setNoCache(boolean noCache) {
        image.build.noCache = noCache;
        return this;
    }

    public TestImageConfigurationBuilder setLabels(Map<String, String> labels) {
        image.build.labels = labels;
        return this;
    }

    public TestImageConfigurationBuilder setContainerfile(String containerfile) {
        image.build.containerFile = containerfile;
        return this;
    }

    public TestImageConfigurationBuilder setContainerfileDir(String containerfileDir) {
        if(containerfileDir != null) {
            image.build.containerFileDir = new File(containerfileDir);
        }
        return this;
    }

    public TestImageConfigurationBuilder setTags(String[] tags) {
        image.build.tags = tags;
        return this;
    }

    public TestImageConfigurationBuilder setUseMavenProjectVersion(boolean useMavenProjectVersion) {
        image.build.tagWithMavenProjectVersion = useMavenProjectVersion;
        return this;
    }

    public TestImageConfigurationBuilder setMavenProjectVersion(String mavenProjectVersion) {
        image.build.mavenProjectVersion = mavenProjectVersion;
        return this;
    }

    public TestImageConfigurationBuilder setCreateLatestTag(boolean createLatestTag) {
        image.build.createLatestTag = createLatestTag;
        return this;
    }

    public TestImageConfigurationBuilder setFormat(ContainerFormat format) {
        image.build.format = format;
        return this;
    }

    public TestImageConfigurationBuilder initAndValidate(MavenProject mavenProject, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        image.initAndValidate(mavenProject, log, failOnMissingContainerfile);
        return this;
    }

    public TestImageConfigurationBuilder addCustomImageNameForBuildStage(String stage, String imageName) {
        StageConfiguration[] currentStagesConfigurations = image.stages;

        if(currentStagesConfigurations == null) {
            currentStagesConfigurations = new StageConfiguration[0];
        }

        List<StageConfiguration> stageConfigurationList = new ArrayList<>(Arrays.asList(currentStagesConfigurations));

        StageConfiguration newStageConfiguration = new StageConfiguration();
        newStageConfiguration.imageName = imageName;
        newStageConfiguration.name = stage;
        stageConfigurationList.add(newStageConfiguration);

        image.stages = stageConfigurationList.toArray(new StageConfiguration[]{});

        return this;
    }

    public ImageConfiguration build() {
        return image;
    }

    public TestImageConfigurationBuilder setUseCustomImageNameForMultiStageContainerfile(boolean useCustomImageNameForMultiStageContainerfile) {
        image.customImageNameForMultiStageContainerfile = useCustomImageNameForMultiStageContainerfile;
        return this;
    }
}
