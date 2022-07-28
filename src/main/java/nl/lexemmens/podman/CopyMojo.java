package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHub;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes a Skopeo copy command, that allows to copy containers from one location
 * to another.
 */
@Mojo(name = "copy", defaultPhase = LifecyclePhase.DEPLOY)
public class CopyMojo extends AbstractCatalogSupport {

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(property = "skopeo.skip.copy", defaultValue = "false")
    boolean skipCopy;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipCopy) {
            getLog().info("Skopeo copy is skipped.");
            return;
        }

        checkAuthentication(hub);
        performCopyUsingCatalogFile(hub);
    }

    @Override
    protected boolean requireImageConfiguration() {
        return false;
    }

    private void copyImage(ServiceHub hub, String sourceImage, String targetImage) throws MojoExecutionException {
        getLog().info(String.format("Copying image %s to %s...", sourceImage, targetImage));
        hub.getSkopeoExecutorService().copy(sourceImage, targetImage);
    }

    private void performCopyUsingCatalogFile(ServiceHub hub) throws MojoExecutionException {
        getLog().info("Using container-catalog.txt to perform Skopeo copy.");

        // Use a customized repository session, setup to force a few behaviors we like.
        PodmanSession tempSession = getTempSession(skopeo.getCopy().getDisableLocal());
        File tempRepo = tempSession.repo;

        List<String> cataloguedImages = readRemoteCatalog(tempSession.session);
        Map<String, String> transformedImages = performTransformation(cataloguedImages);

        for (Map.Entry<String, String> imageEntry : transformedImages.entrySet()) {
            copyImage(hub, imageEntry.getKey(), imageEntry.getValue());
        }

        if (skopeo.getCopy().getDisableLocal() && tempRepo != null) {
            try {
                FileUtils.deleteDirectory(tempRepo);
            } catch (IOException e) {
                getLog().warn("Failed to cleanup temporary repository directory: " + tempRepo);
            }
        }
    }

    private String transformToTargetImageRepo(String sourceImageRepo) {
        return sourceImageRepo.replace(skopeo.getCopy().getSearchString(), skopeo.getCopy().getReplaceString());
    }

    private Map<String, String> performTransformation(List<String> cataloguedImages) {
        Map<String, String> transformedImages = new HashMap<>(cataloguedImages.size());
        for (String image : cataloguedImages) {
            transformedImages.put(
                    image,
                    transformToTargetImageRepo(image)
            );
        }
        return transformedImages;
    }
}
