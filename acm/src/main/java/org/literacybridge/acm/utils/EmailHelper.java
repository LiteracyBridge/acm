package org.literacybridge.acm.utils;

import org.json.simple.JSONObject;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.config.HttpUtility;
import org.literacybridge.acm.gui.assistants.ContentImport.ImportedPage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class EmailHelper {
    private static final Logger LOG = Logger.getLogger(EmailHelper.class.getName());
    @SuppressWarnings("unchecked")
    public static boolean sendEmail(String from, String to, String subject, String body, boolean html) throws
                                                                                                       IOException
    {
        String computerName;
        boolean status_aws = true;

        try {
            computerName = InetAddress
                .getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }

        // send POST request to AWS API gateway to invoke acmCheckOut lambda function
        String requestURL = "https://7z4pu4vzqk.execute-api.us-west-2.amazonaws.com/prod";
        JSONObject request = new JSONObject();

        String db = ACMConfiguration.cannonicalProjectName(ACMConfiguration.getInstance().getCurrentDB().getSharedACMname());
        request.put("db", db);
        request.put("action", "report");
        request.put("name", ACMConfiguration.getInstance().getUserName());
        request.put("contact", ACMConfiguration.getInstance().getUserContact());
        request.put("version", Constants.ACM_VERSION);
        request.put("computername", computerName);
        request.put("from", from);
        request.put("subject", subject);
        request.put("recipient", to);
        request.put("body", body);
        request.put("html", html);

        HttpUtility httpUtility = new HttpUtility();
        JSONObject jsonResponse;
        try {
            httpUtility.sendPostRequest(requestURL, request);
            jsonResponse = httpUtility.readJSONObject();
            LOG.info(String.format("email: %s\n          %s\n", request.toString(), jsonResponse.toString()));
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
        httpUtility.disconnect();

        // parse response
        System.out.println(jsonResponse);

        return status_aws;
    }

    public static BiFunction<TR,Integer,String> blueZebra = new BiFunction<TR,Integer,String>() {
        @Override
        public String apply(TR tr, Integer integer) {
            return "background-color:"+((integer%2==0)?"#fff":"#eff");
        }
    };

    public static BiFunction<TR,Integer,String> pinkZebra = new BiFunction<TR,Integer,String>() {
        @Override
        public String apply(TR tr, Integer integer) {
            return "background-color:"+((integer%2==0)?"#ffeeee":"#ffe0e0");
        }
    };

    public static class TR {
        BiFunction<TR,Integer,String> styler;
        String style;
        List<String> data = new ArrayList<>();
        public TR(String... data) {
            this.data.addAll(Arrays.asList(data));
            this.styler = blueZebra;
        }
        TR append(String... data) {
            this.data.addAll(Arrays.asList(data));
            return this;
        }
        TR withStyle(String style) {
            if (this.style==null) {
                this.style = style;
            } else {
                this.style = this.style+';'+style;
            }
            return this;
        }
        public TR withStyler(BiFunction<TR, Integer, String> styler) {
            this.styler = styler;
            return this;
        }
        public String toString() {
            return format(0);
        }
        public String format(int row) {
            StringBuilder result = new StringBuilder("<tr");
            if (style != null || styler != null) {
                result.append(" style='");
                if (style != null) result.append(style).append(';');
                if (styler!= null) result.append(styler.apply(this, row));
                result.append("'");
            }
            result.append('>');
            data.forEach(s->result.append("<td>").append(s).append("</td>"));
            result.append("</tr>");
            return result.toString();
        }
    }

    public static class HtmlTable {
        List<TR> rows = new ArrayList<>();
        String style;
        public HtmlTable(TR... rows) {
            this.rows.addAll(Arrays.asList(rows));
        }
        public HtmlTable append(TR... rows) {
            this.rows.addAll(Arrays.asList(rows));
            return this;
        }
        public HtmlTable withStyle(String style) {
            if (this.style==null) {
                this.style = style;
            } else {
                this.style = this.style+';'+style;
            }
            return this;
        }
        public String toString() {
            StringBuilder result = new StringBuilder("<table border='1' ");
            if (style != null) result.append(" style='").append(style).append("'");
            result.append('>');
            IntStream.range(0, rows.size())
                .mapToObj(i->rows.get(i).format(i))
                .forEach(result::append);
            result.append("</table>");
            return result.toString();
        }
    }
}
