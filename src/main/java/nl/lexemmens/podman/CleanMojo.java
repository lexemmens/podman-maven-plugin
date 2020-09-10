package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * CleanMojo for building container images using Podman. This Mojo *must* run
 * during the pre-clean phase, when Podman's root storage location is set to be in the target directory.
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class CleanMojo extends AbstractPodmanMojo {

    /**
     * Indicates if cleaning container images should be skipped
     */
    @Parameter(property = "podman.skip.clean.storage", defaultValue = "false")
    boolean skipClean;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipClean) {
            getLog().info("Cleaning local storage is skipped.");
            return;
        }

        if(podman.getRoot() == null) {
            getLog().info("Not cleaning up local storage as default storage location is being used.");
        } else {
            getLog().info("Cleaning up " + podman.getRoot().getAbsolutePath() + "...");
            hub.getBuildahExecutorService().cleanupLocalContainerStorage();
        }
    }

}
