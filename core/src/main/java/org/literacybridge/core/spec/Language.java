package org.literacybridge.core.spec;

import java.text.ParseException;
import java.util.Map;

public class Language {
    final static String[] FILENAMES = new String[] { "pub_languages.csv", "languages.csv", "language_spec.csv" };

    public enum columns {
        code, name
    }

    static String[] columnNames;

    static {
        columnNames = new String[columns.values().length];
        for (int ix = 0; ix < columns.values().length; ix++) {
            columnNames[ix] = columns.values()[ix].name();
        }
    }

    public final String code;
    public final String name;

    public Language(String code,
            String name)
            throws ParseException {
        this.code = code;
        this.name = name;
    }

    public Language(Map<String, String> properties) throws ParseException {
        this.code = properties.get(columns.code.name());
        this.name = properties.get(columns.name.name());
    }

    /**
     * Returns the spec language in the format `code(label)` as used in
     * config.properties
     * eg. en("English"), fr("French")
     */
    @Override
    public String toString() {
        return String.format("%s(\"%s\")", this.code, this.name);
    }
}
