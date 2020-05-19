package nl.lexemmens.podman;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "push", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class PushMojo extends AbstractMojo {

    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.push.skip", required = true )
    private boolean skipPush;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(skipPush){
            getLog().info("Pushing container images is skipped.");
        } else {
            getLog().info("Pushing container image...");
        }
    }
}
