package nl.lexemmens.podman.enumeration;

/**
 * Enumeration that allows specifying the TlsVerify option to use when bulding, pushing or saving
 * container images.
 *
 * Not specifying this explicitly will invoke default behavior (which defaults to true).
 */
public enum TlsVerify {

    NOT_SPECIFIED(""),
    TRUE("--tls-verify=true"),
    FALSE("--tls-verify=false");

    private final String command;

    /**
     * Constructor
     */
    TlsVerify(String command) {
        this.command = command;
    }

    /**
     * Returns the corresponding TLS Verification command matching the selected value
     */
    public String getCommand() {
        return command;
    }

}
