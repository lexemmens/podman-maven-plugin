package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * BuildMojo for building container images using Podman
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class CleanMojo extends AbstractPodmanMojo {

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(property = "podman.skip.clean.storage", defaultValue = "false")
    boolean skipCleanStorage;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipCleanStorage) {
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
