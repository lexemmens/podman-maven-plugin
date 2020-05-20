package nl.lexemmens.podman.service;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that provides access to convenient methods for reading and writing to a file, with convenient
 * error translation for Maven.
 */
public final class IOSupportService {

    private final Log log;

    /**
     * Constructs a new instance of this class.
     *
     * @param log Maven's log system
     */
    public IOSupportService(Log log) {
        this.log = log;
    }

    /**
     * Reads the contents of the provided file and returns a List of Strings, representing the lines of the file.
     *
     * @param file The file to read
     * @return The contents of the file as a List of String
     * @throws MojoExecutionException In case the File could not be read.
     */
    public final List<String> readFileContents(Path file) throws MojoExecutionException {
        try {
            return Files.lines(file).collect(Collectors.toList());
        } catch (IOException e) {
            String msg = String.format("Failed reading contents of file: %s - caught %s", file, e);
            log.error(msg);
            throw new MojoExecutionException(msg);
        }
    }

    /**
     * Transforms the provided path to an actual file on the file system
     *
     * @param file The file to create
     * @return The created file
     * @throws MojoExecutionException In case the file could not be created
     */
    public final Path createFile(Path file) throws MojoExecutionException {
        try {
            return Files.createFile(file);
        } catch (IOException e) {
            String msg = String.format("Failed to filter Docker file. Could not create new file: %s - caught %s", file, e);
            log.error(msg);
            throw new MojoExecutionException(msg);
        }
    }

    /**
     * Writes contents to a file
     *
     * @param file    The file to write to
     * @param content The content to write.
     * @throws MojoExecutionException In case the contents could not be written to the file.
     */
    public final void writeContentsToFile(Path file, List<String> content) throws MojoExecutionException {
        try {
            Files.write(file, content);
        } catch (IOException e) {
            String msg = String.format("Failed to filter Docker file. Could not write to: %s - caught %s", file, e);
            log.error(msg);
            throw new MojoExecutionException(msg);
        }
    }
}
