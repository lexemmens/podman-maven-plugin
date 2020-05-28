package nl.lexemmens.podman;

import nl.lexemmens.podman.enumeration.TlsVerify;
import nl.lexemmens.podman.image.ImageConfiguration;
import nl.lexemmens.podman.service.ServiceHub;
import nl.lexemmens.podman.service.ServiceHubFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;

import java.util.List;

public abstract class AbstractPodmanMojo extends AbstractMojo {

    protected static final String PODMAN = "podman";

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    // Settings holding authentication info
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    /**
     * All the source registries that are required to build and push container images. Note that the target registry must be explicitly set!
     */
    @Parameter(property = "podman.registries")
    protected String[] registries;

    /**
     * The registry of the container images
     */
    @Parameter(property = "podman.push.registry")
    protected String pushRegistry;

    @Parameter
    protected List<ImageConfiguration> images;

    /**
     * Whether Podman should verify TLS/SSL certificates. Defaults to true.
     */
    @Parameter(property = "podman.tls.verify", defaultValue = "NOT_SPECIFIED", required = true)
    protected TlsVerify tlsVerify;

    /**
     * Skip authentication prior to execution
     */
    @Parameter(property = "podman.skip.auth", defaultValue = "false")
    protected boolean skipAuth;

    /**
     * Skip all podman steps
     */
    @Parameter(property = "podman.skip", defaultValue = "false")
    protected boolean skip;

    @Component
    private MavenFileFilter mavenFileFilter;

    @Component
    private ServiceHubFactory serviceHubFactory;

    @Component
    private SettingsDecrypter settingsDecrypter;

    @Override
    public final void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Podman actions are skipped.");
            return;
        }

        ServiceHub hub = serviceHubFactory.createServiceHub(getLog(), project, mavenFileFilter, tlsVerify, settings, settingsDecrypter);

        initImageConfigurations();

        executeInternal(hub);
    }

    private void initImageConfigurations() throws MojoExecutionException {
        getLog().debug("Initializing image configurations.");
        for (ImageConfiguration image : images) {
            image.initAndValidate(project, getLog());
        }
    }

    protected void checkAuthentication(ServiceHub hub) throws MojoExecutionException {
        if (skipAuth) {
            getLog().info("Registry authentication is skipped.");
        } else {
            hub.getAuthenticationService().authenticate(registries);
        }
    }

    /**
     * <p>
     * If the pushRegistry property is set, this method prepends the image name with the value of the pushRegistry
     * </p>
     *
     * @param imageNameWithTag The image name with tag, such as repository/some/image:1.0.0
     * @return The tull image name with the push registry, such as: registry.example.com/repository/some/image:1.0.0
     */
    protected String getFullImageNameWithPushRegistry(String imageNameWithTag) {
        String fullImageName = imageNameWithTag;
        if (pushRegistry != null) {
            fullImageName = String.format("%s/%s", pushRegistry, imageNameWithTag);
        }
        return fullImageName;
    }

    /**
     * Executes this Mojo internally.
     *
     * @param hub A {@link ServiceHub} instance providing access to relevant services
     * @throws MojoExecutionException In case anything happens during execution which prevents execution from continuing
     */
    public abstract void executeInternal(ServiceHub hub) throws MojoExecutionException;
}
