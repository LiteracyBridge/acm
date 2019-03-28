package org.literacybridge.acm.utils;

import org.json.simple.JSONObject;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.config.HttpUtility;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

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
}
