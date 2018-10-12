package org.literacybridge.core.spec;

import java.util.Map;

public class Recipient {
    final static String FILENAME = "recipients.csv";

    private enum columns {
        recipientid, /*project, partner,*/
        communityname,
        groupname, /*affiliate, component,*/
        country,
        region,
        district, /*numhouseholds, */
        numtbs,
        supportentity, /*model,*/
        language, /*coordinates*/
    };

    public static String[] columnNames;

    static {
        columnNames = new String[columns.values().length];
        for (int ix = 0; ix < columns.values().length; ix++) {
            columnNames[ix] = columns.values()[ix].name();
        }
    }

    public final String recipientid;
    public final String communityname;
    public final String groupname;
    public final String country;
    public final String region;
    public final String district;
    public final int numtbs;
    public final String supportentity;
    public final String language;
    
    public Recipient(String recipientid,
        String communityname,
        String groupname,
        String country,
        String region,
        String district,
        String numtbs,
        String supportentity,
        String language)
    {
        this.recipientid = recipientid;
        this.communityname = communityname;
        this.groupname = groupname;
        this.country = country;
        this.region = region;
        this.district = district;
        this.numtbs = Integer.parseInt(numtbs);
        this.supportentity = supportentity;
        this.language = language;
    }

    public Recipient(Map<String, String> properties) {
        this.recipientid = properties.get(columns.recipientid.name());
        this.communityname = properties.get(columns.communityname.name());
        this.groupname = properties.get(columns.groupname.name());
        this.country = properties.get(columns.country.name());
        this.region = properties.get(columns.region.name());
        this.district = properties.get(columns.district.name());
        this.numtbs = Integer.parseInt(properties.get(columns.numtbs.name()));
        this.supportentity = properties.get(columns.supportentity.name());
        this.language = properties.get(columns.language.name());
    }

    public String getName() {
        return String.format("%s / %s / %s / %s / %s / %s",
            country, region, district, communityname, groupname, supportentity);
    }

}
