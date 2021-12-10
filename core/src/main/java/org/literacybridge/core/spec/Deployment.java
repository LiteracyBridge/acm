package org.literacybridge.core.spec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Deployment {
    final static String[] FILENAMES = new String[]{"pub_deployments.csv", "deployment_spec.csv"};

    public enum columns {
        /*project,*/
        /*deployment, deploymentname,*/
        deploymentnumber,
        deployment_num, startdate, enddate, component/*distribution,comment*/
    }

    static String[] columnNames;
    static {
        columnNames = new String[columns.values().length];
        for (int ix = 0; ix < columns.values().length; ix++) {
            columnNames[ix] = columns.values()[ix].name();
        }
    }

    private final String pattern = "yyyy-MM-dd";
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    Date date = simpleDateFormat.parse("2018-09-09");

    public final int deploymentnumber;
    public final Date startdate;
    public final Date enddate;
    public final StringFilter componentFilter;

    public Deployment(String deploymentnumber, String startdate, String enddate, String component)
        throws ParseException
    {
        this.deploymentnumber = Integer.parseInt(deploymentnumber);
        this.startdate = simpleDateFormat.parse(startdate);
        this.enddate = simpleDateFormat.parse(enddate);
        this.componentFilter = new StringFilter(component);
    }

    public Deployment(Map<String, String> properties) throws ParseException {
        String deployment_num_str = properties.get(columns.deployment_num.name());
        this.deploymentnumber = Integer.parseInt(properties.getOrDefault(columns.deploymentnumber.name(), deployment_num_str));
        this.startdate = simpleDateFormat.parse(properties.get(columns.startdate.name()));
        this.enddate = simpleDateFormat.parse(properties.get(columns.enddate.name()));
        this.componentFilter = new StringFilter(properties.get(columns.component.name()));
    }

}
