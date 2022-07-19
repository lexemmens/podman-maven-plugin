package nl.lexemmens.podman.config.skopeo.copy;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Holds the configuration options specifically for using the copy functionality. This included options passed to the
 * skopeo copy command.
 */
public class SkopeoCopyConfiguration {

    /**
     * Only use this repository for retrieving the container catalog file.
     */
    @Parameter(property = "skopeo.copy.source.catalog.repo")
    protected String sourceCatalogRepository;

    /**
     * Disable the local Maven repository during retrieving the container catalog file.
     */
    @Parameter(property = "skopeo.copy.catalog.repo.local.disable", defaultValue = "false")
    protected boolean disableLocal;

    /**
     * This substring of the source container image will be replaced.
     */
    @Parameter(property = "skopeo.copy.image.searchString", required = true)
    protected String searchString;

    /**
     * The searchString substring will be replaced by this string.
     */
    @Parameter(property = "skopeo.copy.image.replaceString", required = true)
    protected String replaceString;

    /**
     * Disable verifying the TLS connection for the source image registry.
     */
    @Parameter(property = "skopeo.copy.srcTlsVerify", defaultValue = "true")
    protected boolean srcTlsVerify;

    /**
     * Disable verifying the TLS connection for the destination image registry.
     */
    @Parameter(property = "skopeo.copy.destTlsVerify", defaultValue = "true")
    protected boolean destTlsVerify;

    public String getSearchString() {
        return searchString;
    }

    public String getReplaceString() {
        return replaceString;
    }

    public String getSourceCatalogRepository() {
        return sourceCatalogRepository;
    }

    public boolean getDisableLocal() {
        return disableLocal;
    }

    public boolean getSrcTlsVerify() {
        return srcTlsVerify;
    }

    public boolean getDestTlsVerify() {
        return destTlsVerify;
    }
}
