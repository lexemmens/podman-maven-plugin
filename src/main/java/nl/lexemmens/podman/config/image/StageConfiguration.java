package nl.lexemmens.podman.config.image;

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

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
