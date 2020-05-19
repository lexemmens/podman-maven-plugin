package nl.lexemmens.podman.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MavenPropertyReader {

    private static final String PROJECT = "project";
    private static final String POM = "pom";
    private static final String PARENT = "parent";

    private final MavenProject project;

    public MavenPropertyReader (MavenProject project) {
        this.project = project;
    }


    public String getProperty(String property) {
        String propertyValue;
        String[] propertyParts = property.split("\\.");
        if(propertyParts.length == 0) {
            propertyValue = null;
        } else {
            propertyValue = getPropertyValue(propertyParts);
        }

        return propertyValue;
    }

    private String getPropertyValue(String[] propertyParts) {
        String propertyValue = null;
        String selector = propertyParts[0];
        switch (selector) {
            case PROJECT:
            case POM:
                propertyValue = recursivelyGetPropertyValue(project, Arrays.copyOfRange(propertyParts, 1, propertyParts.length));
                break;
            case PARENT:
                propertyValue = recursivelyGetPropertyValue(project.getParent(), Arrays.copyOfRange(propertyParts, 1, propertyParts.length));
                break;
            default:
                break;
        }

        return propertyValue;
    }

    private String recursivelyGetPropertyValue(Object holder, String[] parts) {
        for(int i = 0; i < parts.length; i++) {
            try {
                String part = parts[i];
                String methodName = String.format("get%s", StringUtils.capitalize(part));
                Method method = holder.getClass().getMethod(methodName);
                Object result = method.invoke(holder);

                if (i == (parts.length - 1)) {
                    return result.toString();
                } else {
                    return recursivelyGetPropertyValue(result, Arrays.copyOfRange(parts, 1, parts.length));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;

    }



}
