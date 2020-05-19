package nl.lexemmens.podman;

import nl.lexemmens.podman.support.CommandExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

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

    @Parameter(property = "podman.target.repository")
    private String targetRepository;

    @Parameter(property = "podman.image.tag")
    private String[] tags;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipPush) {
            getLog().info("Pushing container images is skipped.");
        } else if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Will not push container images.");
        } else {
            getLog().info("Pushing container images to registry ...");

            CommandExecutor cmdExecutor = new CommandExecutor(getLog());
            for (String tag : tags) {
                getLog().info("Pushing image: " + tag);
                cmdExecutor.runCommand(outputDirectory, PODMAN, PUSH, tag);
            }
        }
    }
}
