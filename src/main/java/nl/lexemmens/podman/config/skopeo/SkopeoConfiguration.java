package nl.lexemmens.podman.config.skopeo;

import nl.lexemmens.podman.config.skopeo.copy.SkopeoCopyConfiguration;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Holds the configuration for calling the skopeo binary.
 */
public class SkopeoConfiguration {
    /**
     * Configuration for the skopeo copy functionality.
     */
    @Parameter
    protected SkopeoCopyConfiguration copy;

    public SkopeoCopyConfiguration getCopy() {
        return copy;
    }
}
