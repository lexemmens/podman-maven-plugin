package nl.lexemmens.podman;

import nl.lexemmens.podman.util.MavenPropertyReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class BuildMojo extends AbstractMojo {

    private static final Path DOCKERFILE = Paths.get("Dockerfile");

    private static final String PODMAN = "podman";
    private static final String TAG = "tag";
    private static final String BUILD = "build";

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    /**
     * Location of project sources
     */
    @Parameter(defaultValue = "${project.basedir}", property = "podman.basedir", required = true)
    private File sourceDirectory;

    /**
     * Indicates if building container images should be skipped
     */
    @Parameter(defaultValue = "false", property = "podman.build.skip")
    private boolean skipBuild;

    /**
     * Skip building tags
     */
    @Parameter(property = "podman.tag.skip", defaultValue = "false")
    private boolean skipTag;

    @Parameter(property = "podman.image.tag")
    private String[] tags;

    private MavenPropertyReader mavenPropertyReader;
    private String imageHash = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipBuild) {
            getLog().info("Building container images is skipped.");
            return;
        }

        Path projectPath = Paths.get(sourceDirectory.toURI());
        Path dockerFile = projectPath.resolve(DOCKERFILE);

        if (!Files.exists(dockerFile)) {
            getLog().info("Project does not have a Dockerfile");
            return;
        }

        if (isDockerfileEmpty(dockerFile)) {
            throw new MojoExecutionException("Dockerfile cannot be empty!");
        }

        mavenPropertyReader = new MavenPropertyReader(project);
        Path targetDockerfile = Paths.get(outputDirectory.toURI()).resolve(DOCKERFILE);

        filterDockerfile(dockerFile, targetDockerfile);
        buildContainerImage(targetDockerfile);
        tagContainerImage(dockerFile);

        getLog().info("Built container image.");
    }

    private void buildContainerImage(Path dockerFile) throws MojoExecutionException {
        try {
            getLog().info("Building container image...");
            ProcessResult process = new ProcessExecutor()
                    .directory(outputDirectory)
                    .command(PODMAN, BUILD, ".")
                    .readOutput(true)
                    .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                    .redirectError(Slf4jStream.of(getClass().getSimpleName()).asError())
                    .exitValueNormal()
                    .execute();

            // We can safely assume exit code 0 at this point
            List<String> processOutput = process.getOutput().getLinesAsUTF8();
            imageHash = processOutput.get(processOutput.size() - 1);
        } catch (IOException | InterruptedException | TimeoutException e) {
            String msg = String.format("Failed to build container image from %s - caught %s", dockerFile, e.getMessage());
            getLog().error(msg);
            throw new MojoExecutionException(msg, e);
        }
    }

    private void tagContainerImage(Path dockerFile) throws MojoExecutionException {
        if (skipTag) {
            getLog().info("Tagging container images is skipped.");
            return;
        }

        if (tags == null || tags.length == 0) {
            getLog().info("No tags specified. Skipping tagging of container images.");
            return;
        }

        if (imageHash == null) {
            getLog().info("No image hash available. Skipping tagging container image.");
        }

        try {
            for (String tag : tags) {
                getLog().info("Applying tag: " + tag);
                ProcessResult process = new ProcessExecutor()
                        .directory(outputDirectory)
                        .command(PODMAN, TAG, imageHash, tag)
                        .readOutput(true)
                        .redirectOutput(Slf4jStream.of(getClass().getSimpleName()).asInfo())
                        .redirectError(Slf4jStream.of(getClass().getSimpleName()).asError())
                        .exitValueNormal()
                        .execute();
            }
        } catch (IOException | InterruptedException | TimeoutException e) {
            String msg = String.format("Failed to build container image from %s - caught %s", dockerFile, e.getMessage());
            getLog().error(msg);
            throw new MojoExecutionException(msg, e);
        }
    }

    private boolean isDockerfileEmpty(Path fullDockerFilePath) {
        try {
            return 0 == Files.size(fullDockerFilePath);
        } catch (IOException e) {
            getLog().error("Unable to determine if Dockerfile is empty.", e);
            return true;
        }
    }

    private void filterDockerfile(Path dockerFile, Path targetDockerfilePath) throws MojoExecutionException {
        try {
            if(Files.exists(targetDockerfilePath)) {
                getLog().info("Dockerfile already exists in target folder.");
            } else {
                List<String> filteredDockerfileContents = getFilteredDockerfileContents(dockerFile);
                getLog().debug("Using target Dockerfile: " + targetDockerfilePath);

                Path targetDockerfile = Files.createFile(targetDockerfilePath);
                if (Files.isWritable(targetDockerfile)) {
                    Files.write(targetDockerfile, filteredDockerfileContents);
                } else {
                    getLog().error("Could not open temporary Dockerfile for writing...");
                }
            }
        } catch (IOException e) {
            String msg = "Failed to read contents of Dockerfile";
            getLog().error(msg);
            throw new MojoExecutionException(msg, e);
        }
    }

    private List<String> getFilteredDockerfileContents(Path dockerfile) throws IOException {
        getLog().debug("Filtering Dockerfile contents...");

        Properties properties = project.getProperties();
        List<String> dockerFileContents = Files.lines(dockerfile).collect(Collectors.toList());
        List<String> targetDockerFileContents = new ArrayList<>();

        String propertyRegex = "\\$\\{[A-Za-z0-9.].*}";
        Pattern propertyPattern = Pattern.compile(propertyRegex);
        for (String line : dockerFileContents) {
            getLog().debug("Processing line " + line);
            Matcher matcher = propertyPattern.matcher(line);
            if (matcher.find()) {
                matcher.reset();
                while (matcher.find()) {
                    String match = matcher.group();
                    Object propertyValue = properties.get(match.substring(2, match.length() - 1));

                    if (propertyValue == null) {
                        propertyValue = mavenPropertyReader.getProperty(match.substring(2, match.length() - 1));
                    }

                    getLog().debug("Replacing '" + match + "' with '" + propertyValue + "'.");
                    targetDockerFileContents.add(line.replaceAll(propertyRegex, propertyValue.toString()));
                }
            } else {
                getLog().debug("Line has no properties. Skipping.");
                targetDockerFileContents.add(line);
            }
        }

        return targetDockerFileContents;

    }

}
