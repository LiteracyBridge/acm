package org.literacybridge.acm.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.HttpUtility;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EmailHelper {
    private static final Logger LOG = Logger.getLogger(EmailHelper.class.getName());
    // The response when the email was sent.
    private static final int EMAIL_SENT_RESPONSE = 200;

    public static boolean sendEmail(String sender, String recipient, String subject, String body, boolean html) {
        List<String> recipientList = Collections.singletonList(recipient);
        return sendEmail(sender, recipientList, subject, body, html);
    }


    @SuppressWarnings("unchecked")
    public static boolean sendEmail(String from, Collection<String> recipientList, String subject, String body, boolean html) {
        Authenticator authenticator = Authenticator.getInstance();
        Authenticator.AwsInterface awsInterface = authenticator.getAwsInterface();
        String computerName;
        boolean status_aws = true;

        try {
            computerName = InetAddress
                .getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }

        // send POST request to AWS API gateway to invoke "report" lambda function
        String requestURL = Authenticator.ACCESS_CONTROL_API + "/report";
        JSONObject requestBody = new JSONObject();

        String db = ACMConfiguration.getInstance().getCurrentDB().getProgramName();
        requestBody.put("db", db);
        requestBody.put("action", "report");
        requestBody.put("name", ACMConfiguration.getInstance().getUserName());
        requestBody.put("contact", ACMConfiguration.getInstance().getUserContact());
        requestBody.put("version", Constants.ACM_VERSION);
        requestBody.put("computername", computerName);
        requestBody.put("from", from);
        requestBody.put("subject", subject);
        JSONArray recipients = new JSONArray();
        recipients.addAll(recipientList);
        requestBody.put("recipient", recipients);
        requestBody.put("body", body);
        requestBody.put("html", html);

        JSONObject jsonResponse;
        jsonResponse = awsInterface.authenticatedPostCall(requestURL, requestBody);
        if (jsonResponse != null) {
            Object o = jsonResponse.get("ResponseMetadata");
            if (o instanceof JSONObject) {
                o = ((JSONObject) o).get("HTTPStatusCode");
            }
            if (o instanceof Long) {
                status_aws = ((Long) o) == EMAIL_SENT_RESPONSE;
            }
            LOG.info(String.format("email: %s\n          %s\n", requestBody.toString(), jsonResponse.toString()));
        }
        // parse response
        System.out.println(jsonResponse);

        return status_aws;
    }

    private static BiFunction<TR, Integer, String> blueZebra = (tr, integer) ->
        "background-color:" + ((integer % 2 == 0) ? "#fff" : "#ebf5fc");

    public static BiFunction<TR, Integer, String> pinkZebra = (tr, integer) ->
        "background-color:" + ((integer % 2 == 0) ? "#ffeeee" : "#ffe0e0");

    /**
     * Sends a summary email report to "interested parties".
     * @param subject - the subject of the email.
     * @param body - the email body.
     */
    public static void sendNotificationEmail(String subject, String body) {
        Collection<String> recipients = ACMConfiguration.getInstance().getCurrentDB().getNotifyList();
        if (recipients.size() > 0) {
            sendEmail("ictnotifications@amplio.org",
                recipients,
                subject,
                body,
                true);
        }
    }

    public static abstract class HtmlElement<T extends HtmlElement> {
        String data;
        Map<String,String> attrs;
        Function<HtmlElement,String> styler;
        String style;
        public HtmlElement() {
            this.data = "";
        }
        public HtmlElement(Object data) {
            this.data = data.toString();
        }
        public T with(String key, Object value) {
            if (attrs == null) attrs = new LinkedHashMap<>();
            attrs.put(key, value.toString());
            //noinspection unchecked
            return (T)this;
        }
        public String toString() {
            StringBuilder result = new StringBuilder("<").append(tag());
            if (attrs != null) {
                attrs.forEach((key, value) -> result.append(' ').append(key)
                    .append("=\"")
                    .append(value)
                    .append('"'));
            }
            if (style != null || styler != null) {
                result.append(" style='");
                if (style != null) result.append(style).append(';');
                if (styler!= null) result.append(styler.apply(this));
                result.append("'");
            }
            result.append('>').append(data).append("</").append(tag()).append('>');
            return result.toString();
        }
        protected abstract String tag();
    }

    public static class TD extends HtmlElement<TD> {
        public TD() {
            super();
        }
        public TD(Object data) {
            super(data);
        }
        protected String tag() {
            return "td";
        }
        public TD colspan(int colspan) {
            return with("colspan", colspan);
        }
    }
    public static class TH extends TD {
        public TH(Object data) {
            super(data);
        }
        public TH() {
            super();
        }
        @Override
        protected String tag() {
            return "th";
        }
    }

    @SuppressWarnings("unused")
    public static class TR {
        BiFunction<TR,Integer,String> styler;
        String style;
        List<TD> data = new ArrayList<>();
        public TR(String... data) {
            this.data.addAll(Arrays.stream(data).map(TD::new).collect(Collectors.toList()));
            this.styler = blueZebra;
        }
        public TR(TD... data) {
            this.data.addAll(Arrays.asList(data));
            this.styler = blueZebra;
        }
        TR append(String... data) {
            this.data.addAll(Arrays.stream(data).map(TD::new).collect(Collectors.toList()));
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
            data.forEach(result::append);
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
