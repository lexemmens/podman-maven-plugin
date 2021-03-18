package nl.lexemmens.podman.helper;

import nl.lexemmens.podman.config.image.single.SingleImageConfiguration;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterReplacer {

    private static final Pattern FORMATTER_PATTERN = Pattern.compile("^(.*?)%([a-z])(.*)$");

    private final Map<String, Replacement> replacementMap;

    public ParameterReplacer(Map<String, Replacement> replacementMap) {
        this.replacementMap = replacementMap;
    }

    public void adaptReplacements(final SingleImageConfiguration imageConfiguration) {
        replacementMap.values().forEach(replacement -> replacement.adaptReplacement(imageConfiguration));
    }

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


    public interface Replacement {

        void adaptReplacement(SingleImageConfiguration imageConfiguration);

        String get();
    }

}
