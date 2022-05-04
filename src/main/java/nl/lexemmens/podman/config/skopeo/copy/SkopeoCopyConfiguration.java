package nl.lexemmens.podman.config.skopeo.copy;

import nl.lexemmens.podman.enumeration.MatchType;
import nl.lexemmens.podman.enumeration.SourceType;
import nl.lexemmens.podman.enumeration.TlsVerify;
import org.apache.maven.plugins.annotations.Parameter;

public class SkopeoCopyConfiguration {

    /**
     * Whether Podman should verify TLS/SSL certificates. Defaults to true.
     */
    @Parameter(property = "skopeo.tls.verify", defaultValue = "NOT_SPECIFIED", required = true)
    protected TlsVerify tlsVerify;

    @Parameter
    protected Integer retryTimes;

    @Parameter
    protected Boolean removeSignatures;

    @Parameter
    protected SkopeoCopySourceConfiguration source;

    @Parameter
    protected SkopeoCopyDestinationConfiguration destination;

    @Parameter(property = "skopeo.match.type", defaultValue = "PARTIAL_STRING", required = true)
    protected MatchType matchType;

    @Parameter(property = "skopeo.copy.source.type", required = true)
    protected SourceType sourceType;

    @Parameter(property = "skopeo.copy.source.catalog.repo")
    protected String sourceCatalogRepository;

    @Parameter(property = "skopeo.copy.catalog.repo.local.disable", defaultValue = "false")
    protected Boolean disableLocal;

    @Parameter(property = "skopeo.copy.image.source", required = true)
    protected String sourceImage;

    @Parameter(property = "skopeo.copy.image.destination", required = true)
    protected String destinationImage;


    public TlsVerify getTlsVerify() {
        return tlsVerify;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public Boolean getRemoveSignatures() {
        return removeSignatures;
    }

    public SkopeoCopySourceConfiguration getSource() {
        return source;
    }

    public SkopeoCopyDestinationConfiguration getDestination() {
        return destination;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceImage() {
        return sourceImage;
    }

    public String getDestinationImage() {
        return destinationImage;
    }

    public String getSourceCatalogRepository() {
        return sourceCatalogRepository;
    }

    public Boolean getDisableLocal() {
        return disableLocal;
    }
}
