package nl.lexemmens.podman.service;

import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationServiceTest {

    @Mock
    private Log log;

    @Mock
    private CommandExecutorService commandExecutorService;

    @Mock
    private Settings settings;

    @Mock
    private SettingsDecrypter settingsDecrypter;

    private final TlsVerify tlsVerify = TlsVerify.TRUE;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void authenticateNullRegistries() {
        AuthenticationService authenticationService = new AuthenticationService(log, commandExecutorService, settings, settingsDecrypter, tlsVerify);
        Assertions.assertThrows(MojoExecutionException.class, () -> authenticationService.authenticate(null));

        verify(log, Mockito.times(1)).info(Mockito.eq("Checking authentication status..."));
        verify(log, Mockito.times(1)).error(Mockito.eq("No registries have been configured but authentication is not skipped. If you want to skip authentication, run again with 'podman.skip.auth' set to true"));
    }

    @Test
    public void authenticateNoRegistries() {
        AuthenticationService authenticationService = new AuthenticationService(log, commandExecutorService, settings, settingsDecrypter, tlsVerify);
        Assertions.assertThrows(MojoExecutionException.class, () -> authenticationService.authenticate(new String[]{}));

        verify(log, Mockito.times(1)).info(Mockito.eq("Checking authentication status..."));
        verify(log, Mockito.times(1)).error(Mockito.eq("No registries have been configured but authentication is not skipped. If you want to skip authentication, run again with 'podman.skip.auth' set to true"));
    }

    @Test
    public void authenticateNoRegistryAuthFileNoEnvVarNoCredentials() throws IOException, MojoExecutionException {
        // Ensure that /run/user/1000/containers/invalidauth.json is not present
        Path authFilePath = Paths.get("/run/user/1000/containers/auth.json");
        Files.deleteIfExists(authFilePath);

        String[] registries = new String[]{"registry.example.com"};

        AuthenticationService authenticationService = new AuthenticationService(log, commandExecutorService, settings, settingsDecrypter, tlsVerify);
        Assertions.assertThrows(MojoExecutionException.class, () -> authenticationService.authenticate(registries));

        verify(log, Mockito.times(1)).info(Mockito.eq("Checking authentication status..."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Authentication file not (yet) present. Authenticating..."));
        verify(log, Mockito.times(0)).error(Mockito.eq("No registries have been configured but authentication is not skipped. If you want to skip authentication, run again with 'podman.skip.auth' set to true"));
    }

    @Test
    public void authenticateNoRegistryAuthFileNoEnvVar() throws IOException, MojoExecutionException {
        String registryName = "registry.example.com";

        Server server = new Server();
        server.setId(registryName);
        server.setUsername("username");
        server.setPassword("password");

        List<Server> serverList = List.of(server);

        when(settings.getServer(eq(registryName))).thenReturn(server);
        when(settings.getServers()).thenReturn(serverList);
        when(settings.getProxies()).thenReturn(new ArrayList<>());
        when(settingsDecrypter.decrypt(isA(SettingsDecryptionRequest.class))).thenReturn(createSettingsDecryptionResult(serverList, new ArrayList<>()));

        // Ensure that /run/user/1000/containers/auth.json is not present
        Path authFilePath = Paths.get("/run/user/1000/containers/auth.json");
        Files.deleteIfExists(authFilePath);

        String[] registries = new String[]{"registry.example.com"};

        AuthenticationService authenticationService = new AuthenticationService(log, commandExecutorService, settings, settingsDecrypter, tlsVerify);
        authenticationService.authenticate(registries);

        verify(log, Mockito.times(1)).info(Mockito.eq("Checking authentication status..."));
        verify(log, Mockito.times(1)).info(Mockito.eq("Authentication file not (yet) present. Authenticating..."));
        verify(log, Mockito.times(0)).error(Mockito.eq("No registries have been configured but authentication is not skipped. If you want to skip authentication, run again with 'podman.skip.auth' set to true"));
        verify(commandExecutorService, times(1)).runCommand(new File("."),
                false,
                true,
                "podman",
                "login",
                "--tls-verify=true",
                registryName,
                "-u",
                "username",
                "-p",
                "password");
    }

    private SettingsDecryptionResult createSettingsDecryptionResult(List<Server> servers, List<Proxy> proxies) {
        return new SettingsDecryptionResult() {
            @Override
            public Server getServer() {
                return null;
            }

            @Override
            public List<Server> getServers() {
                return servers;
            }

            @Override
            public Proxy getProxy() {
                return null;
            }

            @Override
            public List<Proxy> getProxies() {
                return proxies;
            }

            @Override
            public List<SettingsProblem> getProblems() {
                return null;
            }
        };
    }


}
