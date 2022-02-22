package org.literacybridge.core.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class LanguageLabelProvider {
    /**
     * Parses the language labels from the 'AUDIO_LANGUAGES' String property
     * contained in the config.properties file. The appropriate line in the file
     * has the following format:
     * AUDIO_LANGUAGES=en,dga("Dagaare"),twi("Twi"),sfw("Sehwi"),tlh-en("Klingon with EN")
     */
    private final static Pattern LANGUAGE_LABEL_PATTERN = Pattern
        .compile("^([a-zA-Z]{2,}(?:-[a-zA-Z]+)?)(?:\\(\"(.+)\"\\))?$");

    private final List<Locale> audioLanguages = new ArrayList<>();
    private final Map<Locale, String> languageLabels = new HashMap<>();

    /**
     * Initializes a LanguageLabelProvider from a comma-separated list of language codes with optional
     * friendly names, as used in the config.properties files: AUDIO_LANGUAGES=en,dga("Dagaare")
     * @param languagesProperty comma separated list of language codes with optional friendl names.
     */
    public LanguageLabelProvider(String languagesProperty) {
        parseLanguageLabels(languagesProperty);
    }

    /**
     * Gets the friendly name for a language, by locale.
     * @param locale for which language name is desired.
     * @return the friendly name, if one was given, else null.
     */
    public String getLanguageLabel(Locale locale) {
        return languageLabels.get(locale);
    }

    /**
     * Gets the friendly name for a language, by iso code.
     * @param languagecode for which the language name is desired.
     * @return the friendly name, if one was given, else null.
     */
    public String getLanguageLabel(String languagecode) {
        Locale locale = new Locale(languagecode);
        return getLanguageLabel(locale);
    }

    public List<Locale> getAudioLanguages() {
        return Collections.unmodifiableList(audioLanguages);
    }

    private void parseLanguageLabels(String languagesProperty) {
        if (languagesProperty != null) {
            // extract the individual languages from comma-separated list.
            String[] languages = languagesProperty.split(",");
            for (String language : languages) {
                language = language.trim();
                if (isEmpty(language)) continue;
                // Parse into iso code and friendly label.
                Matcher labelMatcher = LANGUAGE_LABEL_PATTERN.matcher(language);
                if (labelMatcher.matches()) {
                    String iso = labelMatcher.group(1);
                    String label = (labelMatcher.groupCount() > 1) ?
                                   labelMatcher.group(2) :
                                   null;
                    RFC3066LanguageCode rfc3066 = new RFC3066LanguageCode(iso);
                    Locale locale = rfc3066.getLocale();
                    if (locale != null) {
                        // Fall back to the locale-provided label, but not the iso code itself; avoid "en (en)" for "en (English):
                        if (isEmpty(label) && !locale.getDisplayName().equalsIgnoreCase(iso)) {
                            label = locale.getDisplayName();
                        }
                        if (!isEmpty(label)) {
                            languageLabels.put(locale, label);
                        }
                        audioLanguages.add(locale);
                    }
                }
            }
            if (audioLanguages.isEmpty()) {
                languageLabels.put(Locale.ENGLISH, Locale.ENGLISH.getDisplayName());
                audioLanguages.add(Locale.ENGLISH);
            }
        }
    }


    public static class RFC3066LanguageCode {
        private final String[] codes;

        public RFC3066LanguageCode(String code) {
            // parse the string
            StringTokenizer tokenizer = new StringTokenizer(code, "-_");
            int numTokens = tokenizer.countTokens();
            codes = new String[numTokens];

            int i = 0;
            while (tokenizer.hasMoreTokens()) {
                codes[i++] = tokenizer.nextToken();
            }
        }

        public static boolean validate(RFC3066LanguageCode code) {
            return code.codes != null && code.codes.length != 0;

            // TODO: validate language and country codes
        }

        public Locale getLocale() {
            switch (codes.length) {
                case 0:
                    return null;
                case 1:
                    return new Locale(codes[0]);
                case 2:
                    return new Locale(codes[0], codes[1]);
                default:
                    return new Locale(codes[0], codes[1], codes[2]);
            }
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(codes[0]);
            for (int i = 1; i < codes.length; i++) {
                b.append("_");
                b.append(codes[i]);
            }
            return b.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof RFC3066LanguageCode)) {
                return false;
            }
            RFC3066LanguageCode other = (RFC3066LanguageCode) obj;
            if (codes.length != other.codes.length) {
                return false;
            }
            for (int ix = 0; ix < codes.length; ix++) {
                if (!codes[ix].equals(other.codes[ix])) {return false;}
            }

            return true;
        }
    }
}
