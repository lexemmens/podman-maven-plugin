package nl.lexemmens.podman.config.image.batch;

import nl.lexemmens.podman.config.image.AbstractImageConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageBuildConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the configuration for the container images that are being built. Values of this class will be set via
 * the Maven pom, except for the image hash.
 */
public class BatchImageConfiguration extends AbstractImageConfiguration<BatchImageBuildConfiguration> {

    /**
     * The build image configuration.
     */
    @Parameter
    protected BatchImageBuildConfiguration build;

    /**
     * Initializes this configuration and fills any null values with default values.
     *
     * @param log                        The log for logging any errors that occur during validation
     * @throws MojoExecutionException In case validation fails.
     */
    public void initAndValidate(Log log, MavenProject project) throws MojoExecutionException {
        super.initAndValidate(log);

        if(build == null) {
            throw new MojoExecutionException("Missing <build/> section in image configuration!");
        }

        build.validate(project);
    }

    /**
     * Concerts this {@link BatchImageConfiguration} into a collection of {@link SingleImageConfiguration} instances. One
     * instance will be created per Containerfile found.
     *
     * @return A collection of {@link SingleImageConfiguration} instances based on the current {@link BatchImageConfiguration}
     * @throws MojoExecutionException In case of an IOException during querying all Containerfiles
     */
    public List<SingleImageConfiguration> resolve(Log log, MavenProject project) throws MojoExecutionException {
        List<SingleImageConfiguration> imageConfigurations = new ArrayList<>();

        if(build.getContainerFileDir() == null) {
            String projectBuildDir = project.getBuild().getDirectory();
            log.info("BATCH > Option containerFileDir not set. Falling back to: " + projectBuildDir);
            build.setContainerFileDir(new File(projectBuildDir));
        }

        List<Path> allContainerFiles = getBuild().getAllContainerFiles();
        if(allContainerFiles == null || allContainerFiles.isEmpty()) {
            throw new MojoExecutionException("Invalid batch configuration found! ");
        }

        log.info("BATCH > Found " + allContainerFiles.size() + " Containerfiles");
        for (Path containerFile : getBuild().getAllContainerFiles()) {
            SingleImageConfiguration imageConfiguration = new SingleImageConfiguration();
            imageConfiguration.setImageName(getImageName());
            imageConfiguration.setCustomImageNameForMultiStageContainerfile(useCustomImageNameForMultiStageContainerfile());
            imageConfiguration.setStages(getStages());

            SingleImageBuildConfiguration buildConfiguration = new SingleImageBuildConfiguration();
            buildConfiguration.setContainerFile(containerFile.getFileName().toString());
            buildConfiguration.setContainerFileDir(containerFile.getParent().toFile());
            buildConfiguration.setFormat(getBuild().getFormat());
            buildConfiguration.setCreateLatestTag(getBuild().isCreateLatestTag());
            buildConfiguration.setLabels(getBuild().getLabels());
            buildConfiguration.setPull(getBuild().isPull());
            buildConfiguration.setNoCache(getBuild().isNoCache());
            buildConfiguration.setTags(getBuild().getTags());
            buildConfiguration.setPullAlways(getBuild().isPullAlways());
            buildConfiguration.setTagWithMavenProjectVersion(getBuild().isTagWithMavenProjectVersion());

            imageConfiguration.setBuild(buildConfiguration);
            imageConfigurations.add(imageConfiguration);
        }

        return imageConfigurations;
    }

    @Override
    public BatchImageBuildConfiguration getBuild() {
        return build;
    }
}
