package nl.lexemmens.podman;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class BuildMojo extends AbstractMojo {

    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.build.skip")
    private boolean skipBuild;

    /**
     * Skip building tags
     */
    @Parameter(property = "podman.tag.skip", defaultValue = "false")
    protected boolean skipTag;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(skipBuild){
            getLog().info("Building container images is skipped.");
        } else {
            getLog().info("Building container image...");
        }
    }
}
