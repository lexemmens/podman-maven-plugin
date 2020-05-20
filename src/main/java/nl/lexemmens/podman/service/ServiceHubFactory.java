package nl.lexemmens.podman.service;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.component.annotations.Component;
import org.apache.maven.project.MavenProject;

/**
 * Factory that allows creation of a new {@link ServiceHub} class
 */
@Component(role = ServiceHubFactory.class, hint = "default")
public class ServiceHubFactory {

    /**
     * Creates a new {@link ServiceHub} instance.
     * @param log Access to Maven's log
     * @param mavenFileFilter The MavenFileFilter instance
     * @return A new instance of the {@link ServiceHub}
     */
    public ServiceHub createServiceHub(Log log, MavenFileFilter mavenFileFilter) {
        return new ServiceHub(log, mavenFileFilter);
    }

}
