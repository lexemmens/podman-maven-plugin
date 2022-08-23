package nl.lexemmens.podman;

import nl.lexemmens.podman.config.image.batch.BatchImageConfiguration;
import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;
import nl.lexemmens.podman.config.podman.PodmanConfiguration;
import nl.lexemmens.podman.config.skopeo.SkopeoConfiguration;
import nl.lexemmens.podman.helper.ImageNameHelper;
import nl.lexemmens.podman.service.ServiceHub;
import nl.lexemmens.podman.service.ServiceHubFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenFileFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractPodmanMojo extends AbstractMojo {

    protected static final String PODMAN_DIRECTORY = "podman";

    protected static final String CATALOG_ARTIFACT_NAME = "container-catalog";

    protected static final String CATALOG_HEADER = "[containers]";

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Holds the authentication data from Maven.
     */
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

    /**
     * Single Image configuration: 1 configuration to 1 image
     */
    @Parameter
    protected List<SingleImageConfiguration> images;

    /**
     * Batch Image Configuration: 1 configuration to n images.
     */
    @Parameter
    protected BatchImageConfiguration batch;

    /**
     * Podman specific configuration
     */
    @Parameter
    protected PodmanConfiguration podman;

    @Parameter
    protected SkopeoConfiguration skopeo;

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

    /**
     * When set to false, the plugin wil not throw an exception if a Containerfile cannot be found. Instead
     * plugin execution will be skipped and a warning is logged.
     */
    @Parameter(property = "podman.fail.on.missing.containerfile", defaultValue = "true")
    protected boolean failOnMissingContainerfile;

    @Component
    private MavenFileFilter mavenFileFilter;

    @Component
    private ServiceHubFactory serviceHubFactory;

    @Component
    private SettingsDecrypter settingsDecrypter;

    @Component
    private MavenProjectHelper mavenProjectHelper;

    protected final List<SingleImageConfiguration> resolvedImages;

    /**
     * Constructor. Initializes this abstract class with a concrete base class
     */
    protected AbstractPodmanMojo() {
        this.resolvedImages = new ArrayList<>();
    }

    @Override
    public final void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("The execution of the podman-maven-plugin is skipped.");
            return;
        }
        if (skipGoal()) {
            getLog().info("The execution of this goal is skipped.");
            return;
        }

        initConfigurations();

        ServiceHub hub = serviceHubFactory.createServiceHub(
                getLog(),
                project,
                mavenFileFilter,
                podman,
                skopeo,
                settings,
                settingsDecrypter,
                mavenProjectHelper
        );

        printPodmanVersion(hub);
        executeInternal(hub);
    }

    private void printPodmanVersion(ServiceHub hub) throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            hub.getPodmanExecutorService().version();
        }
    }

    private void initConfigurations() throws MojoExecutionException {
        getLog().debug("Initializing configurations.");

        if (podman == null) {
            getLog().debug("Using default Podman configuration.");
            podman = new PodmanConfiguration();
        }
        podman.initAndValidate(project, getLog());

        if (skopeo == null) {
            getLog().debug("Using default skopeo configuration.");
            skopeo = new SkopeoConfiguration();
        }

        if (requireImageConfiguration()) {
            initImageConfigurations();
        } else {
            getLog().debug("Validating image configuration is skipped.");
        }
    }

    protected final void initImageConfigurations() throws MojoExecutionException {
        resolveImages();

        if (resolvedImages.isEmpty()) {
            throw new MojoExecutionException("Cannot invoke plugin while there is no image configuration present!");
        } else {
            ImageNameHelper imageNameHelper = new ImageNameHelper(project);
            for (SingleImageConfiguration image : resolvedImages) {
                image.initAndValidate(project, getLog(), failOnMissingContainerfile);

                imageNameHelper.adaptReplacemeents(image);
                imageNameHelper.formatImageName(image);
            }
        }
    }

    private void resolveImages() throws MojoExecutionException {
        if (batch != null) {
            getLog().warn("NOTE: Batch mode enabled.");
            batch.initAndValidate(getLog(), project);
            resolvedImages.addAll(batch.resolve(getLog()));
        }

        if (images != null && !images.isEmpty()) {
            resolvedImages.addAll(images);
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

    /**
     * Check if the execution of the mojo should be skipped
     *
     * @return whether the mojo should be skipped
     */
    protected abstract boolean skipGoal();

    /**
     * Returns whether the image configuration should be initialized
     *
     * @return image configuration must be initialized
     */
    protected boolean requireImageConfiguration() {
        return true;
    }

    protected List<String> singleImageConfigurationToFullImageList(SingleImageConfiguration singleImageConfiguration) {
        List<String> fullImages = new ArrayList<>();
        if (singleImageConfiguration.getBuild().isMultistageContainerFile() && singleImageConfiguration.useCustomImageNameForMultiStageContainerfile()) {
            for (Map.Entry<String, String> stageImage : singleImageConfiguration.getImageHashPerStage().entrySet()) {
                List<String> imageNamesByStage = singleImageConfiguration.getImageNamesByStage(stageImage.getKey());

                if (imageNamesByStage.isEmpty()) {
                    getLog().warn("No image name configured for build stage: " + stageImage.getKey() + "." +
                            "Image " + stageImage.getValue() + " not added to container-catalog.txt!");
                } else {
                    for (String imageName : imageNamesByStage) {
                        String fullImageName = getFullImageNameWithPushRegistry(imageName);
                        fullImages.add(fullImageName);
                    }
                }
            }
        } else {
            if (singleImageConfiguration.getBuild().isMultistageContainerFile()) {
                getLog().warn("Detected multistage Containerfile, but no custom image names have been specified. Falling back to final image.");
            }
            // The image configuration cannot produce an empty list of image names.
            for (String imageNameWithTag : singleImageConfiguration.getImageNames()) {
                String fullImageName = getFullImageNameWithPushRegistry(imageNameWithTag);
                fullImages.add(fullImageName);
            }
        }
        return fullImages;
    }
}
