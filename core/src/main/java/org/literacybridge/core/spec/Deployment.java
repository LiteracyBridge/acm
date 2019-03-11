package org.literacybridge.core.spec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Deployment {
    final static String FILENAME = "deployment_spec.csv";

    public enum columns {
        /*project,*/
        /*deployment, deploymentname,*/
        deployment_num, startdate, enddate, component/*distribution,comment*/
    };
    static String[] columnNames;
    static {
        columnNames = new String[columns.values().length];
        for (int ix = 0; ix < columns.values().length; ix++) {
            columnNames[ix] = columns.values()[ix].name();
        }
    }

    private String pattern = "yyyy-MM-dd";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    Date date = simpleDateFormat.parse("2018-09-09");

    public final int deploymentnumber;
    private final Date startdate;
    private final Date enddate;
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
        this.deploymentnumber = Integer.parseInt(properties.get(columns.deployment_num.name()));
        this.startdate = simpleDateFormat.parse(properties.get(columns.startdate.name()));
        this.enddate = simpleDateFormat.parse(properties.get(columns.enddate.name()));
        this.componentFilter = new StringFilter(properties.get(columns.component.name()));

    }

}
