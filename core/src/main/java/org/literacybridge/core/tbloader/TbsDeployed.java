package org.literacybridge.core.tbloader;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeCsv;

@SuppressWarnings("unused")
public class TbsDeployed extends TbOperation {
    private static final String[] columns = {
        "talkingbookid",
        "recipientid",
        "deployedtimestamp",
        "project",
        "deployment",
        "contentpackage",
        "firmware",
        "location",
        "username",
        "tbcdid",
        "action",
        "newsn",
        "testing",
        "deployment_uuid",
        "latitude",
        "longitude",
    };

    private final String talkingbookid;
    private final String recipientid;
    private final Date deployedtimestamp;
    private final String project;
    private final String deployment;
    private final String contentpackage;
    private final String firmware;
    private final String location;
    private final String username;
    private final String tbcdid;
    private final String action;
    private final String newsn;
    private final String testing;
    private final String deployment_uuid;
    private Float latitude;
    private Float longitude;

    public TbsDeployed(Map<String, String> values) {
        this.talkingbookid = values.get("talkingbookid");
        this.recipientid = values.get("recipientid");
        this.deployedtimestamp = tryParseDate(values.get("deployedtimestamp"));
        this.project = values.get("project");
        this.deployment = values.get("deployment");
        this.contentpackage = values.get("contentpackage");
        this.firmware = values.get("firmware");
        this.location = values.get("location");
        this.username = values.get("username");
        this.tbcdid = values.get("tbcdid");
        this.action = values.get("action");
        this.newsn = values.get("newsn");
        this.testing = values.get("testing");
        this.deployment_uuid = values.get("deployment_uuid");

        if (values.containsKey("latitude") && values.containsKey("longitude")) {
            this.latitude = tryParseFloat(values.get("latitude"));
            this.longitude = tryParseFloat(values.get("longitude"));
        } else if (values.containsKey("coordinates")) {
            Matcher m = TBLoaderConstants.COORDINATES_RE.matcher(values.get("coordinates"));
            if (m.matches()) {
                this.latitude = tryParseFloat(m.group("lat"));
                this.longitude = tryParseFloat(m.group("lon"));
            }
        }

        if (StringUtils.isBlank(this.talkingbookid) || this.deployedtimestamp == null) {
            throw new IllegalArgumentException("TbsDeployed requires a 'talkingbookid' and 'deployedtimestamp'.");
        }
    }


    public String getTalkingbookid() {
        return talkingbookid;
    }

    public String getRecipientid() {
        return recipientid;
    }

    public Date getDeployedtimestamp() {
        return deployedtimestamp;
    }

    public String getProject() {
        return project;
    }

    public String getDeployment() {
        return deployment;
    }

    public String getContentpackage() {
        return contentpackage;
    }

    public String getFirmware() {
        return firmware;
    }

    public String getLocation() {
        return location;
    }

    public String getUsername() {
        return username;
    }

    public String getTbcdid() {
        return tbcdid;
    }

    public String getAction() {
        return action;
    }

    public String getNewsn() {
        return newsn;
    }

    public String getTesting() {
        return testing;
    }

    public String getDeployment_uuid() {
        return deployment_uuid;
    }

    public Float getLatitude() {
        return latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    @Override
    public Date getOperationTimestamp() {
        return getDeployedtimestamp();
    }

    @Override
    OP getOperation() {
        return OP.DEPLOYED;
    }

    public String toString() {
        String[] values = {
            talkingbookid,
            recipientid,
            preferredDateFormat.format(deployedtimestamp),
            project,
            deployment,
            contentpackage,
            firmware,
            location,
            username,
            tbcdid,
            action,
            newsn,
            testing,
            deployment_uuid,
            latitude != null ? latitude.toString() : null,
            longitude != null ? longitude.toString() : null
        };
        return Arrays.stream(values)
            .map(v -> v == null ? "" : escapeCsv(v))
            .collect(Collectors.joining(","));
    }

    public static String header() {
        return Arrays.stream(columns)
            .map(v -> v == null ? "" : escapeCsv(v))
            .collect(Collectors.joining(","));
    }
}
