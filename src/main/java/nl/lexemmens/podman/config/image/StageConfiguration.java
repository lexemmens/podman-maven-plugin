package nl.lexemmens.podman.config.image;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Allows specifying a custom image name for a specific build stage
 * in a Containerfile
 */
public class StageConfiguration {

    @Parameter
    protected String name;

    @Parameter
    protected String imageName;

    /**
     * The name of the stage
     *
     * @return The name of the stage
     */
    public String getName() {
        return name;
    }

    /**
     * The image name to use
     *
     * @return The name of the image to use
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * Sets the name of the image.
     *
     * @param imageName The name of the image to set.
     */
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /**
     * Sets the name of the stage
     *
     * @param stage The name of the stage to set
     */
    public void setStageName(String stage) {
        this.name = stage;
    }
}
