package nl.lexemmens.podman.config.skopeo.copy;

import org.apache.maven.plugins.annotations.Parameter;

public class SkopeoCopyConfiguration {

    @Parameter(property = "skopeo.copy.source.catalog.repo")
    protected String sourceCatalogRepository;

    @Parameter(property = "skopeo.copy.catalog.repo.local.disable", defaultValue = "false")
    protected boolean disableLocal;

    @Parameter(property = "skopeo.copy.image.searchString", required = true)
    protected String searchString;

    @Parameter(property = "skopeo.copy.image.replaceString", required = true)
    protected String replaceString;

    @Parameter(property = "skopeo.copy.srcTlsVerify", defaultValue = "true")
    protected boolean srcTlsVerify;
    
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
