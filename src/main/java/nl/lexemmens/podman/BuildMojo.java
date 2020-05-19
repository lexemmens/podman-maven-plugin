package nl.lexemmens.podman;

import nl.lexemmens.podman.context.PodmanContext;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class BuildMojo extends AbstractMojo {

    private static final Path DOCKERFILE = Paths.get("Dockerfile");

    private static final String PODMAN = "podman";
    private static final String TAG = "tag";
    private static final String BUILD = "build";

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Location of the files - usually the project's target folder
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    /**
     * Location of project sources
     */
    @Parameter(defaultValue = "${project.basedir}", property = "podman.basedir", required = true)
    private File sourceDirectory;

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.build.skip")
    private boolean skipBuild;

    /**
     * Indicates if tagging container images should be skipped
     */
    @Parameter(property = "podman.tag.skip", defaultValue = "false")
    private boolean skipTag;

    /**
     * Array consisting of one or more tags to attach to a container image
     */
    @Parameter(property = "podman.image.tag")
    private String[] tags;

    @Override
    public void execute() throws MojoExecutionException {
        if (skipBuild) {
            getLog().info("Building container images is skipped.");
            return;
        }

        Path projectPath = Paths.get(sourceDirectory.toURI());
        Path dockerFile = projectPath.resolve(DOCKERFILE);
        if (!Files.exists(dockerFile)) {
            getLog().info("Project does not have a Dockerfile");
            return;
        }

        if (isDockerfileEmpty(dockerFile)) {
            throw new MojoExecutionException("Dockerfile cannot be empty!");
        }

        Path targetDockerfile = Paths.get(outputDirectory.toURI()).resolve(DOCKERFILE);

        PodmanContext ctx = new PodmanContext(getLog(), project);
        filterDockerfile(ctx, dockerFile, targetDockerfile);
        buildContainerImage(ctx);
        tagContainerImage(ctx);

        getLog().info("Built container image.");
    }

    private void buildContainerImage(PodmanContext ctx) throws MojoExecutionException {
        getLog().info("Building container image...");

        List<String> processOutput = ctx.getCmdExecutor().runCommand(outputDirectory, PODMAN, BUILD, ".");
        ctx.setImageHash(processOutput.get(processOutput.size() - 1));
    }

    private void tagContainerImage(PodmanContext ctx) throws MojoExecutionException {
        if (skipTag) {
            getLog().info("Tagging container images is skipped.");
            return;
        }

        if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Skipping tagging of container images.");
            return;
        }

        if (ctx.getImageHash().isPresent()) {
            String imageHash = ctx.getImageHash().get();
            for (String tag : tags) {
                getLog().info("Tagging OCI " + imageHash + " as " + tag);

                // Ignore output
                ctx.getCmdExecutor().runCommand(outputDirectory, PODMAN, TAG, imageHash, tag);
            }
        } else {
            getLog().info("No image hash available. Skipping tagging container image.");
        }

    }

    private boolean isDockerfileEmpty(Path fullDockerFilePath) {
        try {
            return 0 == Files.size(fullDockerFilePath);
        } catch (IOException e) {
            getLog().error("Unable to determine if Dockerfile is empty.", e);
            return true;
        }
    }

    private void filterDockerfile(PodmanContext ctx, Path dockerFile, Path targetDockerfilePath) throws MojoExecutionException {
        if (Files.exists(targetDockerfilePath)) {
            getLog().info("Dockerfile already exists in target folder.");
        } else {
            ctx.getFilterSupport().filterDockerfile(dockerFile, targetDockerfilePath, project.getProperties());
        }
    }

}
