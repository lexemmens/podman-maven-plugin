package nl.lexemmens.podman.config.skopeo.copy;

import nl.lexemmens.podman.config.skopeo.TestSkopeoConfigurationBuilder;

import java.io.File;

public class TestSkopeoCopyConfigurationBuilder {
    private final TestSkopeoConfigurationBuilder parent;
    private final SkopeoCopyConfiguration copy;

    public TestSkopeoCopyConfigurationBuilder(TestSkopeoConfigurationBuilder parent, SkopeoCopyConfiguration copy) {
        this.parent = parent;
        this.copy = copy;
    }

    public TestSkopeoConfigurationBuilder closeCopy() {
        return parent;
    }

    public TestSkopeoCopyConfigurationBuilder setSourceCatalogRepository(String sourceCatalogRepository) {
        copy.sourceCatalogRepository = sourceCatalogRepository;
        return this;
    }

    public TestSkopeoCopyConfigurationBuilder setSearchString(String searchString) {
        copy.searchString = searchString;
        return this;
    }

    public TestSkopeoCopyConfigurationBuilder setReplaceString(String replaceString) {
        copy.replaceString = replaceString;
        return this;
    }

    public TestSkopeoCopyConfigurationBuilder setSrcTlsVerify(boolean srcTlsVerify) {
        copy.srcTlsVerify = srcTlsVerify;
        return this;
    }

    public TestSkopeoCopyConfigurationBuilder setDestTlsVerify(boolean destTlsVerify) {
        copy.destTlsVerify = destTlsVerify;
        return this;
    }

    public TestSkopeoCopyConfigurationBuilder setDisableLocal(boolean disableLocal) {
        copy.disableLocal = disableLocal;
        return this;
    }
}
