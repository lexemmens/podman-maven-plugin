package nl.lexemmens.podman.helper;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that helps replacing a character in a String by a character sequence.
 * i.e. %v might return the project version.
 */
public class ParameterReplacer {

    private static final Pattern FORMATTER_PATTERN = Pattern.compile("^([a-z0-9.-_].*?)%([a-z])([a-z0-9.\\-_].*)$");

    private final Map<String, Replacement> replacementMap;

    /**
     * Constructs a new instance of this class
     *
     * @param replacementMap The map of all character replacers, where the key is the character.
     */
    public ParameterReplacer(Map<String, Replacement> replacementMap) {
        this.replacementMap = replacementMap;
    }

    /**
     * Adapt all known replacements based on the supplied {@link SingleImageConfiguration}.
     * <p>
     * Some {@link Replacement} instances may require adaptation as they are dependant on one or more
     * other variables of the {@link SingleImageConfiguration}
     *
     * @param imageConfiguration The {@link SingleImageConfiguration} to adapt the {@link Replacement}s to.
     */
    public void adaptReplacements(final SingleImageConfiguration imageConfiguration) {
        replacementMap.values().forEach(replacement -> replacement.adaptReplacement(imageConfiguration));
    }

    /**
     * Replaces all format parameters in the provided input with their respective replacement
     * value.
     *
     * @param input The input String to process
     * @return The processed String.
     */
    public String replace(String input) {
        StringBuilder ret = new StringBuilder();
        while (true) {
            Matcher matcher = FORMATTER_PATTERN.matcher(input);
            if (!matcher.matches()) {
                ret.append(input);
                return ret.toString();
            }
            ret.append(matcher.group(1));
            ret.append(formatElement(matcher.group(2)));
            input = matcher.group(3);
        }
    }

    private String formatElement(String what) {
        ParameterReplacer.Replacement lookup = replacementMap.get(what);
        if (lookup == null) {
            throw new IllegalArgumentException(String.format("No image name format element '%%%s' known", what));
        }
        String val = lookup.get();
        return String.format("%s", val);
    }

    /**
     * Replacement interface.
     */
    public interface Replacement {

        /**
         * Adapts a {@link Replacement} instance to the provided {@link SingleImageConfiguration}
         *
         * @param imageConfiguration The {@link SingleImageConfiguration} to use.
         */
        void adaptReplacement(SingleImageConfiguration imageConfiguration);

        /**
         * Returns the replacement value for this {@link Replacement} instance
         *
         * @return Its corresponding value.
         */
        String get();
    }

}
