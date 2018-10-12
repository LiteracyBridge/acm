package org.literacybridge.core.spec;

import java.util.Map;

public class RecipientMap {
    final static String FILENAME = "recipients_map.csv";

    public enum columns {
        /*project,*/ directory, recipientid
    };
    public static String[] columnNames;
    static {
        columnNames = new String[RecipientMap.columns.values().length];
        for (int ix = 0; ix< RecipientMap.columns.values().length; ix++) {
            columnNames[ix] = RecipientMap.columns.values()[ix].name();
        }
    }

    public final String recipientid;
    public final String directory;

    public RecipientMap(String recipientid, String directory) {
        this.recipientid = recipientid;
        this.directory = directory;
    }

    public RecipientMap(Map<String, String> properties) {
        this.recipientid = properties.get(columns.recipientid.name());
        this.directory = properties.get(columns.directory.name());
    }

}
