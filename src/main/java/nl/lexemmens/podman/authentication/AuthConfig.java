package nl.lexemmens.podman.authentication;

/**
 * Class that holds authentication information for a specific registry
 */
public class AuthConfig {

    private final String registry;
    private final String username;
    private final String password;

    /**
     * Constructs a new instance of this holder with the specified information
     * @param registry The registry
     * @param username The username to use during authentication
     * @param password The password to use during authentication
     */
    AuthConfig(String registry, String username, String password) {
        this.registry = registry;
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the registry held by this class
     */
    public String getRegistry() {
        return registry;
    }

    /**
     * Returns the username corresponding to this registry
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password corresponding to this registry
     */
    public String getPassword() {
        return password;
    }
}
