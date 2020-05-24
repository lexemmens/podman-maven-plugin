package nl.lexemmens.podman.enumeration;

/**
 * <p>
 * Enumeration that allows specifying the TlsVerify option to use when bulding, pushing or saving
 * container images.
 * </p>
 * <p>
 * Not specifying this explicitly will invoke default behavior (which defaults to true).
 * </p>
 */
public enum TlsVerify {

    /**
     * Default setting.
     */
    NOT_SPECIFIED(""),

    /**
     * Explicitly sets tlsVerify to true
     */
    TRUE("--tls-verify=true"),

    /**
     * Explicitly sets tlsVerifyt to false
     */
    FALSE("--tls-verify=false");

    private final String command;

    /**
     * Constructor.
     */
    TlsVerify(String command) {
        this.command = command;
    }

    /**
     * <p>
     * Returns the corresponding TLS Verification command matching the selected value
     * </p>
     * <p>
     * When the value is {@link TlsVerify#NOT_SPECIFIED} this method returns an empty String
     * </p>
     *
     * @return The corresponding command.
     */
    public String getCommand() {
        return command;
    }

}
