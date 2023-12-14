package nl.lexemmens.podman.enumeration;

/**
 * Defines the pullpolicy of the built image's manifest and configuration data. Recognised values include oci and docker.
 * @see <a href="www.mankier.com/1/podman-build">Manual of podman build</a>
 */
public enum PullPolicy {

    /**
     * Always pull the image and throw an error if the pull fails.
     */
    ALWAYS("always"),
    TRUE("true"),

    /**
     * Only pull the image when it does not exist in the local containers storage. Throw an error if no image is found and the pull fails.
     */
    MISSING("missing"),

    /**
     * Never pull the image but use the one from the local containers storage. Throw an error when no image is found.
     */
    NEVER("never"),
    FALSE("false"),

    /**
     * Pull if the image on the registry is newer than the one in the local containers storage. An image is considered to be newer when the digests are different. Comparing the time stamps is prone to errors. Pull errors are suppressed if a local image was found.
     */
    NEWER("newer");

    private final String pullPolicy;

    /**
     * Constructor
     * @param pullPolicy The pullpolicy to set
     */
    PullPolicy(String pullPolicy) {
        this.pullPolicy = pullPolicy;
    }

    /**
     * Returns the selected pullpolicy
     * @return the selected pullpolicy
     */
    public String getValue() {
        return pullPolicy;
    }
}
