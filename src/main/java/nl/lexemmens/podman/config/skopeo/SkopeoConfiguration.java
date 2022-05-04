package nl.lexemmens.podman.config.skopeo;

import nl.lexemmens.podman.config.skopeo.copy.SkopeoCopyConfiguration;
import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public class SkopeoConfiguration {



    @Parameter
    protected SkopeoCopyConfiguration copy;


    public SkopeoCopyConfiguration getCopy() {
        return copy;
    }
}
