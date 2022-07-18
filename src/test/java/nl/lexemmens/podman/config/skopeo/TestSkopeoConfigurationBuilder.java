package nl.lexemmens.podman.config.skopeo;

import nl.lexemmens.podman.config.skopeo.copy.SkopeoCopyConfiguration;
import nl.lexemmens.podman.config.skopeo.copy.TestSkopeoCopyConfigurationBuilder;

public class TestSkopeoConfigurationBuilder {
    private final SkopeoConfiguration skopeo = new SkopeoConfiguration();

    public TestSkopeoConfigurationBuilder() {
        skopeo.copy = new SkopeoCopyConfiguration();
    }

    public TestSkopeoCopyConfigurationBuilder openCopy() {
        return new TestSkopeoCopyConfigurationBuilder(this, skopeo.copy);
    }

    public SkopeoConfiguration build() {
        return skopeo;
    }
}
