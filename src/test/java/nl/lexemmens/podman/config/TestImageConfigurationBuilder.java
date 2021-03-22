package nl.lexemmens.podman.config;

import nl.lexemmens.podman.config.image.StageConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageBuildConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
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

    private final SingleImageConfiguration image;

    public TestImageConfigurationBuilder(String name) {
        image = new SingleImageConfiguration();
        image.setImageName(name);
        image.setBuild(new SingleImageBuildConfiguration());
    }

    public TestImageConfigurationBuilder setNoCache(boolean noCache) {
        image.getBuild().setNoCache(noCache);
        return this;
    }

    public TestImageConfigurationBuilder setPull(boolean pull) {
        image.getBuild().setPull(pull);
        return this;
    }

    public TestImageConfigurationBuilder setLabels(Map<String, String> labels) {
        image.getBuild().setLabels(labels);
        return this;
    }

    public TestImageConfigurationBuilder setContainerfile(String containerfile) {
        image.getBuild().setContainerFile(containerfile);
        return this;
    }

    public TestImageConfigurationBuilder setContainerfileDir(String containerfileDir) {
        if (containerfileDir != null) {
            image.getBuild().setContainerFileDir(new File(containerfileDir));
        }
        return this;
    }

    public TestImageConfigurationBuilder setTags(String[] tags) {
        image.getBuild().setTags(tags);
        return this;
    }

    public TestImageConfigurationBuilder setUseMavenProjectVersion(boolean useMavenProjectVersion) {
        image.getBuild().setTagWithMavenProjectVersion(useMavenProjectVersion);
        return this;
    }

    public TestImageConfigurationBuilder setCreateLatestTag(boolean createLatestTag) {
        image.getBuild().setCreateLatestTag(createLatestTag);
        return this;
    }

    public TestImageConfigurationBuilder setFormat(ContainerFormat format) {
        image.getBuild().setFormat(format);
        return this;
    }

    public TestImageConfigurationBuilder initAndValidate(MavenProject mavenProject, Log log, boolean failOnMissingContainerfile) throws MojoExecutionException {
        image.initAndValidate(mavenProject, log, failOnMissingContainerfile);
        return this;
    }

    public TestImageConfigurationBuilder addCustomImageNameForBuildStage(String stage, String imageName) {
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

    public TestImageConfigurationBuilder setPullAlways(boolean pullAlways) {
        image.getBuild().setPullAlways(pullAlways);
        return this;
    }

    public SingleImageConfiguration build() {
        return image;
    }

    public TestImageConfigurationBuilder setUseCustomImageNameForMultiStageContainerfile(boolean useCustomImageNameForMultiStageContainerfile) {
        image.setCustomImageNameForMultiStageContainerfile(useCustomImageNameForMultiStageContainerfile);
        return this;
    }
}
