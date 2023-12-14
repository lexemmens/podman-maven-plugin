package nl.lexemmens.podman.config.image.batch;

import nl.lexemmens.podman.config.image.StageConfiguration;
import nl.lexemmens.podman.enumeration.ContainerFormat;
import nl.lexemmens.podman.enumeration.PullPolicy;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestBatchImageConfigurationBuilder {

    private final BatchImageConfiguration image;

    public TestBatchImageConfigurationBuilder(String name) {
        image = new BatchImageConfiguration();
        image.setImageName(name);
        image.build = new BatchImageBuildConfiguration();
    }

    public TestBatchImageConfigurationBuilder(String name, BatchImageBuildConfiguration build) {
        image = new BatchImageConfiguration();
        image.setImageName(name);
        image.build = build;
    }

    public TestBatchImageConfigurationBuilder setStages(StageConfiguration[] stages) {
        image.setStages(stages);
        return this;
    }

    public TestBatchImageConfigurationBuilder setNoCache(boolean noCache) {
        image.getBuild().setNoCache(noCache);
        return this;
    }

    public TestBatchImageConfigurationBuilder setPullPolicy(PullPolicy pullPolicy) {
        image.getBuild().setPullPolicy(pullPolicy);
        return this;
    }

    public TestBatchImageConfigurationBuilder setLabels(Map<String, String> labels) {
        image.getBuild().setLabels(labels);
        return this;
    }

    public TestBatchImageConfigurationBuilder setContainerfile(String containerfile) {
        image.getBuild().setContainerFile(containerfile);
        return this;
    }

    public TestBatchImageConfigurationBuilder setContainerfileDir(String containerfileDir) {
        if (containerfileDir != null) {
            image.getBuild().setContainerFileDir(new File(containerfileDir));
        }
        return this;
    }

    public TestBatchImageConfigurationBuilder setTags(String[] tags) {
        image.getBuild().setTags(tags);
        return this;
    }

    public TestBatchImageConfigurationBuilder setUseMavenProjectVersion(boolean useMavenProjectVersion) {
        image.getBuild().setTagWithMavenProjectVersion(useMavenProjectVersion);
        return this;
    }

    public TestBatchImageConfigurationBuilder setCreateLatestTag(boolean createLatestTag) {
        image.getBuild().setCreateLatestTag(createLatestTag);
        return this;
    }

    public TestBatchImageConfigurationBuilder setFormat(ContainerFormat format) {
        image.getBuild().setFormat(format);
        return this;
    }

    public TestBatchImageConfigurationBuilder initAndValidate(MavenProject mavenProject, Log log) throws MojoExecutionException {
        image.initAndValidate(log, mavenProject);
        return this;
    }

    public TestBatchImageConfigurationBuilder addCustomImageNameForBuildStage(String stage, String imageName) {
        StageConfiguration[] currentStagesConfigurations = image.getStages();

        if(currentStagesConfigurations == null) {
            currentStagesConfigurations = new StageConfiguration[0];
        }

        List<StageConfiguration> stageConfigurationList = new ArrayList<>(Arrays.asList(currentStagesConfigurations));

        StageConfiguration newStageConfiguration = new StageConfiguration();
        newStageConfiguration.setImageName(imageName);
        newStageConfiguration.setStageName(stage);
        stageConfigurationList.add(newStageConfiguration);

        image.setStages(stageConfigurationList.toArray(new StageConfiguration[]{}));

        return this;
    }

    public BatchImageConfiguration build() {
        return image;
    }

    public TestBatchImageConfigurationBuilder setUseCustomImageNameForMultiStageContainerfile(boolean useCustomImageNameForMultiStageContainerfile) {
        image.setCustomImageNameForMultiStageContainerfile(useCustomImageNameForMultiStageContainerfile);
        return this;
    }
}
