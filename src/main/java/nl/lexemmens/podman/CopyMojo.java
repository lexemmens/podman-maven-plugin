package nl.lexemmens.podman;

import nl.lexemmens.podman.service.ServiceHub;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
    private RepositorySystem repositorySystem;

    @Override
    public void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if (skipCopy) {
            getLog().info("Skopeo Copy is skipped.");
            return;
        }

        checkAuthentication(hub);

        switch (skopeo.getCopy().getSourceType()) {
            case CATALOG_FILE:
                performCopyUsingCatalogFile(hub);
                break;
            case CONFIGURATION:
                performCopyUsingImageConfiguration();
                break;
            default:
                getLog().warn(
                        "Unknown source type: " + skopeo.getCopy().getSourceType() + ". Skipping Skopeo copy."
                );
        }
    }

    @Override
    protected boolean requireImageConfiguration() {
        return false;
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
            } catch (Exception ex) {
                getLog().warn("Failed to disable local repository path.", ex);
            }
        }
        tempSession.setReadOnly();

        List<ArtifactRepository> remoteArtifactRepositories = getRemoteArtifactRepositories();
        List<String> cataloguedImages = getCatalog(remoteArtifactRepositories);
        Map<String, String> transformedImages = performTransformation(cataloguedImages);

        for(Map.Entry<String, String> imageEntry : transformedImages.entrySet()) {
            hub.getSkopeoExecutorService().copy(imageEntry.getKey(), imageEntry.getValue());
        }

        if (skopeo.getCopy().getDisableLocal()) {
            if (tempRepo != null) {
                try {
                    FileUtils.deleteDirectory(tempRepo);
                } catch (IOException e) {
                    getLog().warn("Failed to cleanup temporary repository directory: " + tempRepo);
                }
            }
        }
    }

    private void performCopyUsingImageConfiguration() throws MojoExecutionException {
        getLog().info("Using image configuration to perform Skopeo copy.");
        getLog().info("Lazy loading image configuration...");
        super.initImageConfigurations();
    }

    private List<ArtifactRepository> getRemoteArtifactRepositories() throws MojoExecutionException {
        List<ArtifactRepository> remoteArtifactRepositories;
        String sourceCatalogRepository = skopeo.getCopy().getSourceCatalogRepository();

        if(sourceCatalogRepository == null) {
            getLog().info("Using all remote repositories to find container catalog.");
            remoteArtifactRepositories = project.getRemoteArtifactRepositories();
        } else {
            Optional<ArtifactRepository> repository = project.getRemoteArtifactRepositories()
                    .stream()
                    .filter(r -> r.getId().equals(sourceCatalogRepository))
                    .findFirst();
            if(repository.isPresent()) {
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

        return remoteArtifactRepositories;
    }

    private List<String> getCatalog(List<ArtifactRepository> remoteRepositories) throws MojoExecutionException {
        // Locate our text catalog classifier file. :-)
        Artifact artifact = repositorySystem.createArtifactWithClassifier(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                "txt",
                CATALOG_ARTIFACT_NAME
        );

        RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
        repositoryRequest.setRemoteRepositories(remoteRepositories);
        repositoryRequest.setForceUpdate(true);

        ArtifactResolutionRequest artifactResolutionRequest = new ArtifactResolutionRequest(repositoryRequest);
        artifactResolutionRequest.setArtifact(artifact);

        ArtifactResolutionResult artifactResolutionResult = repositorySystem.resolve(artifactResolutionRequest);
        if (artifactResolutionResult.isSuccess()) {
            Set<Artifact> resolvedArtifacts = artifactResolutionResult.getArtifacts();
            Optional<Artifact> firstResolvedArtifact = resolvedArtifacts.stream().findFirst();
            if (firstResolvedArtifact.isPresent()) {
                Path resolvedArtifactPath = Paths.get(firstResolvedArtifact.get().getFile().toURI());
                try(Stream<String> catalogStream = Files.lines(resolvedArtifactPath)) {
                    return catalogStream.skip(1).collect(Collectors.toList());
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to read container catalog.", e);
                }
            } else {
                throw new MojoExecutionException("Cannot find container catalog. All repositories were successfully " +
                        "queried, but no such artifact was returned.");
            }
        } else {
            throw new MojoExecutionException("Failed to query repositories for container catalog.");
        }
    }

    private Map<String, String> performTransformation(List<String> cataloguedImages) {
        return null;
    }
}
