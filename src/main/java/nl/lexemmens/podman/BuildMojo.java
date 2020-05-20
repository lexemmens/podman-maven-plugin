package nl.lexemmens.podman;

import nl.lexemmens.podman.context.BuildContext;
import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Mojo(name = "build", defaultPhase = LifecyclePhase.INSTALL)
public class BuildMojo extends AbstractPodmanMojo {

    private static final String PODMAN = "podman";
    private static final String TAG = "tag";
    private static final String BUILD = "build";

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(property = "podman.skip.build", defaultValue = "false")
    private boolean skipBuild;

    /**
     * Indicates if tagging container images should be skipped
     */
    @Parameter(property = "podman.skip.tag", defaultValue = "false")
    private boolean skipTag;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipBuild) {
            getLog().info("Building container images is skipped.");
            return;
        }

        BuildContext buildContext = getBuildContext();
        buildContext.validate();

        filterDockerfile(buildContext, hub);
        buildContainerImage(buildContext, hub);
        tagContainerImage(buildContext, hub);

        getLog().info("Built container image.");
    }

    private void filterDockerfile(BuildContext buildContext, ServiceHub hub) throws MojoExecutionException {
        getLog().debug("Filtering Dockerfile...");
        hub.getFileFilterService().filterDockerfile(buildContext);
    }

    private void buildContainerImage(BuildContext buildContext, ServiceHub hub) throws MojoExecutionException {
        getLog().info("Building container image...");

        List<String> processOutput = hub.getCommandExecutorService().runCommand(outputDirectory, PODMAN, BUILD, ".");
        buildContext.setImageHash(processOutput.get(processOutput.size() - 1));
    }

    private void tagContainerImage(BuildContext buildContext, ServiceHub hub) throws MojoExecutionException {
        if (skipTag) {
            getLog().info("Tagging container images is skipped.");
            return;
        }

        if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Skipping tagging of container images.");
            return;
        }

        if (buildContext.getImageHash().isPresent()) {
            String imageHash = buildContext.getImageHash().get();
            for (String tag : tags) {
                getLog().info("Tagging container image " + imageHash + " as " + tag);

                // Ignore output
                hub.getCommandExecutorService().runCommand(outputDirectory, PODMAN, TAG, imageHash, tag);
            }
        } else {
            getLog().info("No image hash available. Skipping tagging container image.");
        }
    }

    private BuildContext getBuildContext() {
        Path projectPath = Paths.get(sourceDirectory.toURI());
        Path sourceDockerfile = projectPath.resolve(DOCKERFILE);
        Path targetDockerfile = Paths.get(outputDirectory.toURI()).resolve(DOCKERFILE);

        return new BuildContext(sourceDockerfile, targetDockerfile, getLog(), project);
    }
}
