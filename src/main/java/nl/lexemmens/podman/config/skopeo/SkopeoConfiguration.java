package nl.lexemmens.podman.config.skopeo;

import nl.lexemmens.podman.config.skopeo.copy.SkopeoCopyConfiguration;
import org.apache.maven.plugins.annotations.Parameter;

public class SkopeoConfiguration {
    @Parameter
    protected SkopeoCopyConfiguration copy;

    public SkopeoCopyConfiguration getCopy() {
        return copy;
    }
}
