package nl.lexemmens.podman.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Support class that helps reading Maven specific properties such as ${project.artifactId}
 */
public class MavenPropertyReader {

    private static final String PROJECT = "project";
    private static final String POM = "pom";
    private static final String PARENT = "parent";

    private final MavenProject project;

    /**
     * Constructs a new instance of this reader
     * @param project The MavenProject that contains the property values
     */
    public MavenPropertyReader (MavenProject project) {
        this.project = project;
    }

    /**
     * Retrieves the value of a specific property from the MavenProject.
     *
     * @param property The property to retrieve
     * @return The property value or null in case the property has no value
     * @throws MojoExecutionException When the property cannot be read from the MavenProject
     */
    public String getProperty(String property) throws MojoExecutionException {
        String propertyValue;
        String[] propertyParts = property.split("\\.");
        if(propertyParts.length == 0) {
            propertyValue = null;
        } else {
            propertyValue = getPropertyValue(propertyParts);
        }

        return propertyValue;
    }

    private String getPropertyValue(String[] propertyParts) throws MojoExecutionException {
        String propertyValue = null;
        String selector = propertyParts[0];
        switch (selector) {
            case PROJECT:
            case POM:
                propertyValue = safelyGetPropertyValue(project, Arrays.copyOfRange(propertyParts, 1, propertyParts.length));
                break;
            case PARENT:
                propertyValue = safelyGetPropertyValue(project.getParent(), Arrays.copyOfRange(propertyParts, 1, propertyParts.length));
                break;
            default:
                break;
        }

        return propertyValue;
    }

    private String safelyGetPropertyValue(Object holder, String[] parts) throws MojoExecutionException {
        try {
            return recursivelyGetPropertyValue(holder, parts);
        } catch(Exception e) {
            throw new MojoExecutionException("Failed to property " + StringUtils.join(parts, "."));
        }
    }

    private String recursivelyGetPropertyValue(Object holder, String[] parts) throws Exception {
        for(int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String methodName = String.format("get%s", StringUtils.capitalize(part));
            Method method = holder.getClass().getMethod(methodName);
            Object result = method.invoke(holder);

            if (i == (parts.length - 1)) {
                return result.toString();
            } else {
                return recursivelyGetPropertyValue(result, Arrays.copyOfRange(parts, 1, parts.length));
            }
        }

        return null;
    }



}
