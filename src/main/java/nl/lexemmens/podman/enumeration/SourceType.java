package nl.lexemmens.podman.enumeration;

/**
 * Indicates where the Skopeop copy Mojo should retrieve its images from
 */
public enum SourceType {

    /**
     * Attempts to resolve the catalog file from the
     */
    // TODO Add to docs: Catalog file required for batch processing
    CATALOG_FILE,

    /**
     * Matches using {@link String#contains(CharSequence)}
     */
    CONFIGURATION;

}
