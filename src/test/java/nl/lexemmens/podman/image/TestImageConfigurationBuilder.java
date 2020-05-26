package nl.lexemmens.podman.image;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Map;

public class TestImageConfigurationBuilder {

    private final ImageConfiguration image;

    public TestImageConfigurationBuilder(String name) {
        image = new ImageConfiguration();
        image.name = name;
        image.build = new BuildImageConfiguration();
    }

    public TestImageConfigurationBuilder setNoCache(boolean noCache) {
        image.build.noCache = noCache;
        return this;
    }

    public TestImageConfigurationBuilder setLabels(Map<String, String> labels) {
        image.build.labels = labels;
        return this;
    }

    public TestImageConfigurationBuilder setDockerfile(String dockerfile) {
        image.build.dockerFile = dockerfile;
        return this;
    }

    public TestImageConfigurationBuilder setDockerfileDir(String dockerfileDir) {
        if(dockerfileDir != null) {
            image.build.dockerFileDir = new File(dockerfileDir);
        }
        return this;
    }

    public TestImageConfigurationBuilder setTags(String[] tags) {
        image.build.tags = tags;
        return this;
    }

    public TestImageConfigurationBuilder setUseMavenProjectVersion(boolean useMavenProjectVersion) {
        image.build.useMavenProjectVersion = useMavenProjectVersion;
        return this;
    }

    public TestImageConfigurationBuilder setMavenProjectVersion(String mavenProjectVersion) {
        image.build.mavenProjectVersion = mavenProjectVersion;
        return this;
    }

    public TestImageConfigurationBuilder setCreateLatestTag(boolean createLatestTag) {
        image.build.createLatestTag = createLatestTag;
        return this;
    }

    public TestImageConfigurationBuilder initAndValidate(MavenProject mavenProject, Log log) throws MojoExecutionException {
        image.initAndValidate(mavenProject, log);
        return this;
    }

    public ImageConfiguration build() {
        return image;
    }

}
