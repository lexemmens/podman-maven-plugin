package nl.lexemmens.podman.enumeration;

/**
 * Indicates a match type for a copy action using Skopeo copy
 */
public enum MatchType {

    /**
     * Matches using {@link String#equals(Object)}
     */
    PRECISE,

    /**
     * Matches using {@link String#contains(CharSequence)}
     */
    PARTIAL,

    /**
     * Matches using a regular expression
     */
    REGEX
}
