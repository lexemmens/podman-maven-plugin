package nl.lexemmens.podman.image;

import org.apache.maven.plugins.annotations.Parameter;

public class StageConfiguration {

    @Parameter
    protected String name;

    @Parameter
    protected String imageName;

    public String getName() {
        return name;
    }

    public String getImageName() {
        return imageName;
    }
}
