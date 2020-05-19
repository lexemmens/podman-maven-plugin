package nl.lexemmens.podman;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Mojo(name = "push", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class PushMojo extends AbstractMojo {

    private static final String PODMAN = "podman";
    private static final String PUSH = "push";


    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.push.skip", required = true)
    private boolean skipPush;

    @Parameter(property = "podman.image.tag")
    private String[] tags;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipPush) {
            getLog().info("Pushing container images is skipped.");
        } else if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Will not push container images.");
        } else {
            getLog().info("Pushing container image...");

            for (String tag : tags) {
                try {
                    getLog().info("Pushing image: " + tag);
                    ProcessResult process = new ProcessExecutor()
                            .directory(outputDirectory)
                            .command(PODMAN, PUSH, tag)
                            .readOutput(true)
                            .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                            .redirectError(Slf4jStream.of(getClass().getSimpleName()).asError())
                            .exitValueNormal()
                            .execute();

                } catch (IOException | InterruptedException | TimeoutException e) {
                    String msg = String.format("Failed to push container image %s to the registry: %s", tag, e.getMessage());
                    getLog().error(msg);
                    throw new MojoExecutionException(msg, e);
                }
            }
        }
    }
}
