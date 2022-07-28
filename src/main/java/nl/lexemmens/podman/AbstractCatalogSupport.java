package nl.lexemmens.podman;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractCatalogSupport extends AbstractPodmanMojo {

    @Parameter(defaultValue = "${repositorySystemSession}", required = true)
    public RepositorySystemSession repositorySystemSession;

    @Component
    public EnhancedLocalRepositoryManagerFactory localRepositoryManagerFactory;

    @Component
    public RepositorySystem repositorySystem;

    protected List<String> readLocalCatalog() throws MojoExecutionException {
        String catalogFileName = String.format("%s.txt", CATALOG_ARTIFACT_NAME);
        Path catalogPath = Paths.get(project.getBuild().getDirectory(), catalogFileName);
        return readCatalogContent(catalogPath, true);
    }

    protected List<String> readRemoteCatalog(RepositorySystemSession repositorySystemSession) throws MojoExecutionException {
        List<RemoteRepository> remoteRepositories = getRemoteRepositories();

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
                return readCatalogContent(Paths.get(artifactResult.getArtifact().getFile().toURI()), false);
            } else {
                throw new MojoExecutionException("Failed to resolve the container catalog file.");
            }

        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Failed retrieving container catalog file", e);
        }
    }

    private List<String> readCatalogContent(Path catalogPath, boolean local) throws MojoExecutionException {
        try (Stream<String> catalogStream = Files.lines(catalogPath)) {
            return catalogStream.skip(1)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            String msg = "Failed to read container catalog.";
            if (local) {
                msg += " Make sure the build goal is executed.";
            }
            getLog().error(msg);
            throw new MojoExecutionException(msg, e);
        }
    }

    protected List<RemoteRepository> getRemoteRepositories() throws MojoExecutionException {
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

    protected PodmanSession getTempSession(boolean disableLocalRepo) {
        // Use a customized repository session, setup to force a few behaviors we like.
        DefaultRepositorySystemSession tempSession = new DefaultRepositorySystemSession(repositorySystemSession);
        tempSession.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);

        File tempRepo = null;
        if (disableLocalRepo) {
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

        return new PodmanSession(tempSession, tempRepo);
    }

    public static final class PodmanSession {
        public final DefaultRepositorySystemSession session;
        public final File repo;

        public PodmanSession(DefaultRepositorySystemSession session, File repo) {
            this.session = session;
            this.repo = repo;
        }
    }

}
