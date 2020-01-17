package org.literacybridge.core.spec;

import java.util.Map;

public class Recipient {
    final static String FILENAME = "recipients.csv";

    private enum columns {
        recipientid, /*project, partner,*/
        communityname,
        groupname, /*affiliate,*/
        component,
        country,
        region,
        district, /*numhouseholds, */
        numtbs,
        supportentity, /*model,*/
        language, /*coordinates*/
        agent, /*,latitude,longitude,*/
        variant
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
    public final String component;
    public final String country;
    public final String region;
    public final String district;
    public final int numtbs;
    public final String supportentity;
    public final String language;
    public final String agent;
    public final String variant;
    
    public Recipient(Map<String, String> properties) {
        this.recipientid = properties.get(columns.recipientid.name());
        this.communityname = properties.get(columns.communityname.name());
        this.groupname = properties.get(columns.groupname.name());
        this.component = properties.get(columns.component.name());
        this.country = properties.get(columns.country.name());
        this.region = properties.get(columns.region.name());
        this.district = properties.get(columns.district.name());
        this.numtbs = Integer.parseInt(properties.get(columns.numtbs.name()));
        String agentKey = properties.containsKey(columns.agent.name()) ? columns.agent.name() : columns.supportentity.name();
        this.agent = properties.get(agentKey);
        this.supportentity = properties.get(columns.supportentity.name());
        this.language = properties.get(columns.language.name());
        this.variant = properties.getOrDefault(columns.variant.name(), "");
    }

    public String getName() {
        return String.format("%s / %s / %s / %s / %s / %s",
            country, region, district, communityname, groupname, agent);
    }

}
