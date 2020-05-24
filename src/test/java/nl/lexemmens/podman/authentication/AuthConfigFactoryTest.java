package nl.lexemmens.podman.authentication;

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
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class AuthConfigFactoryTest {

    @Mock
    private Settings settings;

    @Mock
    private SettingsDecrypter settingsDecrypter;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testEmptyServers() {
        when(settings.getServers()).thenReturn(new ArrayList<>());
        when(settings.getProxies()).thenReturn(new ArrayList<>());
        when(settingsDecrypter.decrypt(isA(SettingsDecryptionRequest.class))).thenReturn(createSettingsDecryptionResult(new ArrayList<>(), new ArrayList<>()));

        AuthConfigFactory authConfigFactory = new AuthConfigFactory(settings, settingsDecrypter);
        Optional<AuthConfig> authConfigForRegistry = authConfigFactory.getAuthConfigForRegistry("registry.example.com");
        Assertions.assertFalse(authConfigForRegistry.isPresent());
    }

    @Test
    public void testServerNotInSettings() {
        Server server = new Server();
        server.setId("the-incorrect-registry.example.com");
        server.setUsername("username");
        server.setUsername("password");

        List<Server> serverList = List.of(server);

        when(settings.getServers()).thenReturn(serverList);
        when(settings.getProxies()).thenReturn(new ArrayList<>());
        when(settingsDecrypter.decrypt(isA(SettingsDecryptionRequest.class))).thenReturn(createSettingsDecryptionResult(serverList, new ArrayList<>()));

        AuthConfigFactory authConfigFactory = new AuthConfigFactory(settings, settingsDecrypter);
        Optional<AuthConfig> authConfigForRegistry = authConfigFactory.getAuthConfigForRegistry("registry.example.com");
        Assertions.assertFalse(authConfigForRegistry.isPresent());
    }

    @Test
    public void testServerInSettings() {
        Server server = new Server();
        server.setId("registry.example.com");
        server.setUsername("username");
        server.setPassword("password");

        List<Server> serverList = List.of(server);

        when(settings.getServer(eq("registry.example.com"))).thenReturn(server);
        when(settings.getServers()).thenReturn(serverList);
        when(settings.getProxies()).thenReturn(new ArrayList<>());
        when(settingsDecrypter.decrypt(isA(SettingsDecryptionRequest.class))).thenReturn(createSettingsDecryptionResult(serverList, new ArrayList<>()));

        AuthConfigFactory authConfigFactory = new AuthConfigFactory(settings, settingsDecrypter);
        Optional<AuthConfig> authConfigForRegistry = authConfigFactory.getAuthConfigForRegistry("registry.example.com");
        Assertions.assertTrue(authConfigForRegistry.isPresent());
        AuthConfig authConfig = authConfigForRegistry.get();
        Assertions.assertEquals("registry.example.com", authConfig.getRegistry());
        Assertions.assertEquals("username", authConfig.getUsername());
        Assertions.assertEquals("password", authConfig.getPassword());
    }

    /**
     * Fair and square: According to the implementation of the SettingsDecrypter, this can never happen.
     */
    @Test
    public void testServerNotInDecryptedSettings() {
        Server incorrectServer = new Server();
        incorrectServer.setId("the-incorrect-registry.example.com");
        incorrectServer.setUsername("username");
        incorrectServer.setUsername("password");
        List<Server> incorrectServerList = List.of(incorrectServer);

        Server requestedServer= new Server();
        requestedServer.setId("registry.example.com");
        requestedServer.setUsername("username");
        requestedServer.setUsername("password");

        List<Server> requestedServerList = List.of(requestedServer);

        when(settings.getServers()).thenReturn(requestedServerList);
        when(settings.getServer(eq("registry.example.com"))).thenReturn(requestedServer);
        when(settings.getProxies()).thenReturn(new ArrayList<>());

        // This can never happen according to the SettingsDecrypter's implementation, but lets proof we are robust against it anyway.
        when(settingsDecrypter.decrypt(isA(SettingsDecryptionRequest.class))).thenReturn(createSettingsDecryptionResult(incorrectServerList, new ArrayList<>()));

        AuthConfigFactory authConfigFactory = new AuthConfigFactory(settings, settingsDecrypter);
        Optional<AuthConfig> authConfigForRegistry = authConfigFactory.getAuthConfigForRegistry("registry.example.com");
        Assertions.assertFalse(authConfigForRegistry.isPresent());
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
