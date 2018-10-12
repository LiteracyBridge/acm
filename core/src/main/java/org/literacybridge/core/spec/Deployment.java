package org.literacybridge.core.spec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Deployment {
    public final static String FILENAME = "deployments.csv";

    public enum columns {
        /*project,*/
        deployment, /*deploymentname,*/
        deploymentnumber, startdate, enddate, /*distribution,comment*/
    };
    public static String[] columnNames;
    static {
        columnNames = new String[RecipientMap.columns.values().length];
        for (int ix = 0; ix < RecipientMap.columns.values().length; ix++) {
            columnNames[ix] = RecipientMap.columns.values()[ix].name();
        }
    }

    String pattern = "yyyy-MM-dd";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    Date date = simpleDateFormat.parse("2018-09-09");

    public final String deployment;
    public final int deploymentnumber;
    public final Date startdate;
    public final Date enddate;

    public Deployment(String deployment, String deploymentnumber, String startdate, String enddate)
        throws ParseException
    {
        this.deployment = deployment;
        this.deploymentnumber = Integer.parseInt(deploymentnumber);
        this.startdate = simpleDateFormat.parse(startdate);
        this.enddate = simpleDateFormat.parse(enddate);
    }

    public Deployment(Map<String, String> properties) throws ParseException {
        this.deployment = properties.get(columns.deployment.name());
        this.deploymentnumber = Integer.parseInt(properties.get(columns.deploymentnumber.name()));
        this.startdate = simpleDateFormat.parse(properties.get(columns.startdate.name()));
        this.enddate = simpleDateFormat.parse(properties.get(columns.enddate.name()));
    }

}
