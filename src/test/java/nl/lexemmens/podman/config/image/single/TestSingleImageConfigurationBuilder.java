package nl.lexemmens.podman.config.image.single;

import nl.lexemmens.podman.config.image.StageConfiguration;
import nl.lexemmens.podman.enumeration.ContainerFormat;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestSingleImageConfigurationBuilder {

    private final SingleImageConfiguration image;

    public TestSingleImageConfigurationBuilder(String name) {
        image = new SingleImageConfiguration();
        image.setImageName(name);
        image.setBuild(new SingleImageBuildConfiguration());
    }

    public TestSingleImageConfigurationBuilder setNoCache(boolean noCache) {
        image.getBuild().setNoCache(noCache);
        return this;
    }

    public TestSingleImageConfigurationBuilder setPull(boolean pull) {
        image.getBuild().setPull(pull);
        return this;
    }

    public TestSingleImageConfigurationBuilder setLabels(Map<String, String> labels) {
        image.getBuild().setLabels(labels);
        return this;
    }

    public TestSingleImageConfigurationBuilder setContainerfile(String containerfile) {
        image.getBuild().setContainerFile(containerfile);
        return this;
    }

    public TestSingleImageConfigurationBuilder setContainerfileDir(String containerfileDir) {
        if (containerfileDir != null) {
            image.getBuild().setContainerFileDir(new File(containerfileDir));
        }
        return this;
    }

    public TestSingleImageConfigurationBuilder setTags(String[] tags) {
        image.getBuild().setTags(tags);
        return this;
    }

    public TestSingleImageConfigurationBuilder setUseMavenProjectVersion(boolean useMavenProjectVersion) {
        image.getBuild().setTagWithMavenProjectVersion(useMavenProjectVersion);
        return this;
    }

    public TestSingleImageConfigurationBuilder setCreateLatestTag(boolean createLatestTag) {
        image.getBuild().setCreateLatestTag(createLatestTag);
        return this;
    }

    public TestSingleImageConfigurationBuilder setFormat(ContainerFormat format) {
        image.getBuild().setFormat(format);
        return this;
    }

    public TestSingleImageConfigurationBuilder initAndValidate(MavenProject mavenProject, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        image.initAndValidate(mavenProject, log, failOnMissingContainerfile);
        return this;
    }

    public TestSingleImageConfigurationBuilder addCustomImageNameForBuildStage(String stage, String imageName) {
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

    public TestSingleImageConfigurationBuilder setPullAlways(boolean pullAlways) {
        image.getBuild().setPullAlways(pullAlways);
        return this;
    }

    public SingleImageConfiguration build() {
        return image;
    }

    public TestSingleImageConfigurationBuilder setUseCustomImageNameForMultiStageContainerfile(boolean useCustomImageNameForMultiStageContainerfile) {
        image.setCustomImageNameForMultiStageContainerfile(useCustomImageNameForMultiStageContainerfile);
        return this;
    }
}
