package nl.lexemmens.podman;

import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.podman.TestPodmanConfigurationBuilder;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.config.skopeo.TestSkopeoConfigurationBuilder;
import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class CopyMojoTest extends AbstractMojoTest {

    @InjectMocks
    CopyMojo copyMojo;

    private void configureMojo(boolean skipAll, boolean skipCopy, boolean skipAuth, String sourceCatalogRepository, String[] registries, String searchString, String replaceString, String format, boolean srcTlsVerify, boolean destTlsVerify, boolean disableLocal) {
        copyMojo.skipCopy = skipCopy;
        copyMojo.skipAuth = skipAuth;
        copyMojo.podman = new TestPodmanConfigurationBuilder().setTlsVerify(TlsVerify.NOT_SPECIFIED).build();
        copyMojo.skopeo = new TestSkopeoConfigurationBuilder()
                .openCopy()
                .setSourceCatalogRepository(sourceCatalogRepository)
                .setSearchString(searchString)
                .setReplaceString(replaceString)
                .setSrcTlsVerify(srcTlsVerify)
                .setDestTlsVerify(destTlsVerify)
                .setDisableLocal(disableLocal)
                .closeCopy()
                .build();
        copyMojo.skip = skipAll;
        copyMojo.registries = registries;
    }

    @Before
    public void prepare() throws IOException, ArtifactResolutionException, NoLocalRepositoryManagerException {
        Path containerCatalogPath = Paths.get("target/podman-test/container-catalog.txt");
        Files.createDirectories(containerCatalogPath);
        Files.copy(
                ClassLoader.getSystemClassLoader().getResourceAsStream("copy/container-catalog.txt"),
                containerCatalogPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        ArtifactRepository artifactRepository1 = new MavenArtifactRepository();
        artifactRepository1.setLayout(new DefaultRepositoryLayout());
        artifactRepository1.setId("foo");

        ArtifactRepository artifactRepository2 = new MavenArtifactRepository();
        artifactRepository2.setLayout(new DefaultRepositoryLayout());
        artifactRepository2.setId("bar");
        List<ArtifactRepository> artifactRepositories = Arrays.asList(artifactRepository1, artifactRepository2);

        Artifact containerCatalogArtifact = new DefaultArtifact("com.example", "foo", "container-catalog", "txt", "0.1.0");
        containerCatalogArtifact = containerCatalogArtifact.setFile(containerCatalogPath.toFile());

        when(serviceHub.getSkopeoExecutorService()).thenReturn(skopeoExecutorService);
        when(serviceHubFactory.createServiceHub(isA(Log.class), isA(MavenProject.class), isA(MavenFileFilter.class), isA(PodmanConfiguration.class), isA(SkopeoConfiguration.class), isA(Settings.class), isA(SettingsDecrypter.class), isA(MavenProjectHelper.class))).thenReturn(serviceHub);

        when(mavenProject.getRemoteArtifactRepositories()).thenReturn(artifactRepositories);
        copyMojo.repositorySystem = mock(RepositorySystem.class);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        ArtifactResult artifactResult = new ArtifactResult(artifactRequest);
        artifactResult.setArtifact(containerCatalogArtifact);
        when(copyMojo.repositorySystem.resolveArtifact(any(RepositorySystemSession.class), any(ArtifactRequest.class))).thenReturn(artifactResult);

        DefaultRepositorySystemSession defaultRepositorySystemSession = new DefaultRepositorySystemSession();
        LocalRepository localRepository = new LocalRepository(new File("target/podman-test"));
        copyMojo.localRepositoryManagerFactory = new EnhancedLocalRepositoryManagerFactory();
        LocalRepositoryManager localRepositoryManager = copyMojo.localRepositoryManagerFactory.newInstance(defaultRepositorySystemSession, localRepository);
        defaultRepositorySystemSession.setLocalRepositoryManager(localRepositoryManager);
        copyMojo.repositorySystemSession = defaultRepositorySystemSession;
    }

    @Test
    public void testSkipCopy() throws MojoExecutionException {
        configureMojo(false, true, true, null, new String[]{}, "stage", "release", null, false, false, false);
        assertDoesNotThrow(copyMojo::execute);
        verify(skopeoExecutorService, never()).copy(anyString(), anyString());
    }

    @Test
    public void testLocalDisabled() throws ArtifactResolutionException, MojoExecutionException {
        configureMojo(false, false, true, null, null, "stage", "release", null, false, false, true);
        // We can pass an empty ArtifactRequest, since the method is mocked anyway
        ArtifactResult artifactResult = copyMojo.repositorySystem.resolveArtifact(copyMojo.repositorySystemSession, new ArtifactRequest());
        when(copyMojo.repositorySystem.resolveArtifact(isA(RepositorySystemSession.class), isA(ArtifactRequest.class)))
                .thenAnswer(invocation -> {
                    RepositorySystemSession repositorySystemSession = invocation.getArgument(0);
                    LocalRepository localRepository = repositorySystemSession.getLocalRepository();
                    File localRepositoryDir = localRepository.getBasedir();
                    assertTrue(localRepositoryDir.getAbsolutePath().startsWith("/tmp"), "Since we disabled the local repository, a repository in /tmp must be used");
                    return artifactResult;
                });
        assertDoesNotThrow(copyMojo::execute);
        verify(skopeoExecutorService, times(1)).copy("dep1.stage.registry.example.com/foo/bar:0.1.0", "dep1.release.registry.example.com/foo/bar:0.1.0");
        verify(skopeoExecutorService, times(1)).copy("dep2.stage.registry.example.com/project/product:2.1.3", "dep2.release.registry.example.com/project/product:2.1.3");
    }

    @Test
    public void testSetSourceCatalogRepository() throws ArtifactResolutionException, MojoExecutionException {
        String sourceCatalogRepository = "bar";
        configureMojo(false, false, true, sourceCatalogRepository, null, "stage", "release", null, false, false, true);
        // We can pass an empty ArtifactRequest, since the method is mocked anyway

        ArtifactResult artifactResult = copyMojo.repositorySystem.resolveArtifact(copyMojo.repositorySystemSession, new ArtifactRequest());
        when(copyMojo.repositorySystem.resolveArtifact(isA(RepositorySystemSession.class), isA(ArtifactRequest.class)))
                .thenAnswer(invocation -> {
                    ArtifactRequest artifactRequest = invocation.getArgument(1);
                    List<RemoteRepository> remoteRepositories = artifactRequest.getRepositories();
                    assertEquals(1, remoteRepositories.size(), "Only one remote repository should be in");
                    assertEquals(sourceCatalogRepository, remoteRepositories.get(0).getId(), "The ID should equal the sourceCatalogRepository specified");
                    return artifactResult;
                });
        assertDoesNotThrow(copyMojo::execute);
        verify(skopeoExecutorService, times(1)).copy("dep1.stage.registry.example.com/foo/bar:0.1.0", "dep1.release.registry.example.com/foo/bar:0.1.0");
        verify(skopeoExecutorService, times(1)).copy("dep2.stage.registry.example.com/project/product:2.1.3", "dep2.release.registry.example.com/project/product:2.1.3");
    }

    @Test
    public void testNoRepoAndLocalDisabledFail() {
        configureMojo(false, false, true, "fake", null, "stage", "release", null, false, false, true);
        assertThrows(MojoExecutionException.class, copyMojo::execute);
    }

    @Test
    public void testWithCatalogFile() throws MojoExecutionException {
        configureMojo(false, false, true, null, new String[]{}, "stage", "release", null, false, false, false);
        assertDoesNotThrow(copyMojo::execute);
        verify(skopeoExecutorService, times(0)).copy(anyString(), anyString());
    }

    @Test
    public void testSkipCopyNoCatalogFile() throws ArtifactResolutionException {
        configureMojo(false, false, true, null, new String[]{}, "stage", "release", null, false, false, false);
        when(copyMojo.repositorySystem.resolveArtifact(any(RepositorySystemSession.class), any(ArtifactRequest.class)))
            .thenThrow(new ArtifactResolutionException(null, null, new ArtifactNotFoundException(null, null)));
        assertDoesNotThrow(copyMojo::execute);
    }

    private static void cleanDir(Path dir) throws IOException {
        LinkedList<IOException> ioExceptions = new LinkedList<>();
        Files.list(dir).forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    cleanDir(path);
                }
                Files.delete(path);
            } catch (IOException e) {
                ioExceptions.add(e);
            }
        });
        if (!ioExceptions.isEmpty()) {
            IOException lastIOException = ioExceptions.removeLast();
            for (IOException ioException : ioExceptions) {
                ioException.printStackTrace();
            }
            throw lastIOException;
        }
    }

    @After
    public void cleanup() throws IOException {
        cleanDir(Paths.get("target/podman-test"));
    }
}
