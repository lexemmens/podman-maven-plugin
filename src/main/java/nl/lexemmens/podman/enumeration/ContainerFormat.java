package nl.lexemmens.podman.enumeration;

/**
 * Defines the format of the built image's manifest and configuration data. Recognised values include oci and docker.
 * @see <a href="www.mankier.com/1/podman-build">Manual of podman build</a>
 */
public enum ContainerFormat {

    /**
     * Sets the format of the built image's manifest and configuration data to 'oci' (OCI image-spec v1.0). This is the default.
     */
    OCI("oci"),

    /**
     * Sets the format of the built image's manifest and configuration data to 'docker' (version 2, using schema format 2 for the manifest).
     */
    DOCKER("docker");

    private final String format;

    /**
     * Constructor
     * @param format The format to set
     */
    ContainerFormat(String format) {
        this.format = format;
    }

    /**
     * Returns the selected format
     * @return the selected format
     */
    public String getValue() {
        return format;
    }
}
