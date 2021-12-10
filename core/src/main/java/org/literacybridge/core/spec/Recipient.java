package org.literacybridge.core.spec;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Recipient {
    final static String[] FILENAMES = new String[]{"pub_recipients.csv", "recipients.csv"};
    final static Pattern DEPLOYMENTS_LIST = Pattern.compile("^\\[?([0-9, ]*)]?$");

    // We don't care about most of the recipient columns in the ACM or TB-Loader.
    enum columns {
        recipientid, /*project, partner,*/
        communityname,
        groupname, /*affiliate,*/
        component,
        country,
        region,
        district, /*numhouseholds, */
        numtbs,
        supportentity, /*model,*/
        language, // deprecated in favor of languagecode
        languagecode, /*coordinates*/
        agent, /*,latitude,longitude,*/
        variant, /* group_size */
        deployments, /* agent_gender, direct_beneficiaries,direct_beneficiaries_additional,indirect_beneficiaries */
    }

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
    public final String languagecode;
    public final String agent;
    public final String variant;
    public List<Integer> deployments;
    
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
        String language = properties.get(columns.language.name());
        this.languagecode = properties.getOrDefault(columns.languagecode.name(), language).toLowerCase();
        this.variant = properties.getOrDefault(columns.variant.name(), "").toLowerCase();
        // If deployments is present and consists of a list of integers, save that list.
        String deployments_property = properties.getOrDefault(columns.deployments.name(), "");
        Matcher deployments_matcher = DEPLOYMENTS_LIST.matcher(deployments_property);
        if (deployments_matcher.matches()) {
            String deployments_list = deployments_matcher.group(1).trim();
            List<Integer> depls = Arrays.stream(deployments_list.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            if (depls.size() > 0) {
                this.deployments = depls;
            }
        }
    }

    public String getName() {
        return String.format("%s / %s / %s / %s / %s / %s",
            country, region, district, communityname, groupname, agent);
    }

}
