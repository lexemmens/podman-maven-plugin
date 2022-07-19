package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHub;
import org.apache.commons.io.FileUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Executes a Skopeo copy command, that allows to copy containers from one location
 * to another.
 */
@Mojo(name = "copy", defaultPhase = LifecyclePhase.DEPLOY)
public class CopyMojo extends AbstractPodmanMojo {

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(property = "skopeo.skip.copy", defaultValue = "false")
    boolean skipCopy;

    @Parameter(defaultValue = "${repositorySystemSession}", required = true)
    RepositorySystemSession repositorySystemSession;

    @Component
    EnhancedLocalRepositoryManagerFactory localRepositoryManagerFactory;

    @Component
    RepositorySystem repositorySystem;

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
        DefaultRepositorySystemSession tempSession = new DefaultRepositorySystemSession(repositorySystemSession);
        tempSession.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);

        File tempRepo = null;
        if (skopeo.getCopy().getDisableLocal()) {
            getLog().info("Disabling local repository @ " + tempSession.getLocalRepository().getBasedir());
            try {
                tempRepo = Files.createTempDirectory("podman-maven-plugin-repo").toFile();

                getLog().info("Using temporary local repository @ " + tempRepo.getAbsolutePath());
                tempSession.setLocalRepositoryManager(localRepositoryManagerFactory.newInstance(tempSession, new LocalRepository(tempRepo)));
            } catch (IOException | NoLocalRepositoryManagerException e) {
                getLog().warn("Failed to disable local repository path.", e);
            }
        }
        tempSession.setReadOnly();

        List<RemoteRepository> remoteRepositories = getRemoteRepositories();
        List<String> cataloguedImages = getCatalog(tempSession, remoteRepositories);
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

    private List<RemoteRepository> getRemoteRepositories() throws MojoExecutionException {
        List<ArtifactRepository> remoteArtifactRepositories;
        String sourceCatalogRepository = skopeo.getCopy().getSourceCatalogRepository();

        if (sourceCatalogRepository == null) {
            getLog().info("Using all remote repositories to find container catalog.");
            remoteArtifactRepositories = project.getRemoteArtifactRepositories();
        } else {
            Optional<ArtifactRepository> repository = project.getRemoteArtifactRepositories()
                    .stream()
                    .filter(repo -> repo.getId().equals(sourceCatalogRepository))
                    .findFirst();
            if (repository.isPresent()) {
                getLog().info("Using repository " + repository.get() + " for finding container catalog.");
                remoteArtifactRepositories = new ArrayList<>();
                remoteArtifactRepositories.add(repository.get());
            } else if (skopeo.getCopy().getDisableLocal()) {
                throw new MojoExecutionException("Cannot resolve artifacts from 'null' repository if the local repository is also disabled.");
            } else {
                getLog().debug("Resolving container images via catalog from local repository only.");
                remoteArtifactRepositories = new ArrayList<>();
            }
        }

        return RepositoryUtils.toRepos(remoteArtifactRepositories);
    }

    private List<String> getCatalog(RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories) throws MojoExecutionException {
        try {
            DefaultArtifact artifact = new DefaultArtifact(
                    project.getGroupId(),
                    project.getArtifactId(),
                    CATALOG_ARTIFACT_NAME,
                    "txt",
                    project.getVersion()
            );

            ArtifactRequest artifactRequest = new ArtifactRequest(artifact, remoteRepositories, null);

            ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
            if (artifactResult.isMissing()) {
                throw new MojoExecutionException("Cannot find container catalog. All repositories were successfully " +
                        "queried, but no such artifact was returned.");
            }
            if (artifactResult.isResolved()) {
                Path resolvedArtifactPath = Paths.get(artifactResult.getArtifact().getFile().toURI());
                try (Stream<String> catalogStream = Files.lines(resolvedArtifactPath)) {
                    return catalogStream.skip(1).collect(Collectors.toList());
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to read container catalog.", e);
                }
            } else {
                throw new MojoExecutionException("Failed to resolve the container catalog file.");
            }

        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Failed retrieving container catalog file", e);
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
